package com.bsdcampus.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.util.LruCache
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache
import com.bsdcampus.app.BuildConfig
import com.bsdcampus.app.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

class PermanentMediaException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

data class UploadTargetToken(
    val pathname: String,
    val token: String
)

object CampusMediaRepository {
    private const val VERCEL_BLOB_BASE_URL = "https://blob.vercel-storage.com/"
    private val imageMemoryCache = LruCache<String, ByteArray>(24)
    private val naturalAspectRatioCache = ConcurrentHashMap<String, Float>()

    private fun mediaTokenEndpointUrlOrNull(): String? =
        BuildConfig.MEDIA_TOKEN_ENDPOINT_URL.trim().takeIf {
            it.startsWith("https://", ignoreCase = true) ||
                it.startsWith("http://", ignoreCase = true)
        }

    private fun blobReadWriteTokenOrNull(): String? =
        BuildConfig.BLOB_READ_WRITE_TOKEN.trim().takeIf {
            it.isNotBlank() && it != "BLOB_READ_WRITE_TOKEN_PLACEHOLDER"
        }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun getDb(context: Context): FirebaseFirestore =
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))

    fun clearMemoryCache() {
        imageMemoryCache.evictAll()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun evictCachedImage(context: Context?, uriOrUrl: String?) {
        if (uriOrUrl.isNullOrBlank()) return
        val assetId = MediaUtils.extractAssetId(uriOrUrl)
        if (assetId != null) {
            imageMemoryCache.remove(assetId)
            CampusChunkedVideoCache.evictAsset(context, assetId)
            if (context != null) {
                val dir = mediaCacheDir(context)
                runCatching { File(dir, "$assetId.jpg").delete() }
                runCatching { File(dir, "${assetId}_thumb.jpg").delete() }
            }
            return
        }
        if (context != null && (uriOrUrl.startsWith("http://", ignoreCase = true) ||
                uriOrUrl.startsWith("https://", ignoreCase = true))
        ) {
            runCatching {
                val loader = Coil.imageLoader(context.applicationContext)
                loader.memoryCache?.remove(MemoryCache.Key(uriOrUrl))
                loader.diskCache?.remove(uriOrUrl)
            }
        }
    }

    private fun mediaCacheDir(context: Context): File =
        File(context.cacheDir, "campus_media").apply { mkdirs() }

    private suspend fun resolveUploadTargets(
        auth: FirebaseAuth,
        requests: List<Pair<String, String>>
    ): Result<List<UploadTargetToken>> = withContext(Dispatchers.IO) {
        if (requests.isEmpty()) return@withContext Result.success(emptyList())

        val endpoint = mediaTokenEndpointUrlOrNull()
        if (endpoint != null) {
            val user = auth.currentUser
                ?: return@withContext Result.failure(
                    PermanentMediaException("Sign in required before uploading media.")
                )
            var forceRefresh = false
            for (attempt in 0..1) {
                val idToken = runCatching { user.getIdToken(forceRefresh).await().token }
                    .getOrNull()?.takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(
                        PermanentMediaException("Unable to verify Firebase session for upload.")
                    )

                val payload = JSONObject().apply {
                    put("files", JSONArray().apply {
                        requests.forEach { (pathname, mimeType) ->
                            put(JSONObject().apply {
                                put("pathname", pathname)
                                put("contentType", mimeType)
                            })
                        }
                    })
                }.toString()

                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        doInput = true
                        useCaches = false
                        connectTimeout = 8_000
                        readTimeout = 15_000
                        setRequestProperty("Authorization", "Bearer $idToken")
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                    val code = conn.responseCode
                    if (code == 401 && attempt == 0) {
                        forceRefresh = true
                        continue
                    }

                    val responseBody = runCatching {
                        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                        stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }.getOrDefault("")

                    if (code !in 200..299) {
                        val errMsg = runCatching {
                            JSONObject(responseBody).optString("error")
                        }.getOrDefault("").ifBlank { "Upload token request failed (HTTP $code)" }
                        val err = if (code in 400..499 && code != 408 && code != 429) {
                            PermanentMediaException(errMsg)
                        } else {
                            IllegalStateException(errMsg)
                        }
                        return@withContext Result.failure(err)
                    }

                    val json = JSONObject(responseBody)
                    val arr = json.optJSONArray("tokens")
                    if (arr != null && arr.length() == requests.size) {
                        val parsed = (0 until arr.length()).map { idx ->
                            val obj = arr.getJSONObject(idx)
                            val token = obj.optString("clientToken").trim()
                            val path = obj.optString("pathname").ifBlank { requests[idx].first }
                            if (token.isBlank()) {
                                return@withContext Result.failure(
                                    PermanentMediaException("Upload backend returned an empty client token.")
                                )
                            }
                            UploadTargetToken(pathname = path, token = token)
                        }
                        return@withContext Result.success(parsed)
                    } else if (requests.size == 1) {
                        val singleToken = json.optString("clientToken").trim()
                        val singlePath = json.optString("pathname").ifBlank { requests[0].first }
                        if (singleToken.isNotBlank()) {
                            return@withContext Result.success(
                                listOf(UploadTargetToken(pathname = singlePath, token = singleToken))
                            )
                        }
                    }
                    return@withContext Result.failure(
                        PermanentMediaException("Upload backend returned an invalid token payload.")
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    return@withContext Result.failure(
                        if (t is Exception) t else IllegalStateException(t.message ?: "Token exchange failed", t)
                    )
                } finally {
                    runCatching { conn?.disconnect() }
                }
            }
        }

        val rwToken = blobReadWriteTokenOrNull()
            ?: return@withContext Result.failure(
                PermanentMediaException(
                    "Upload backend not configured. Deploy vercel-backend & set MEDIA_TOKEN_ENDPOINT_URL, or configure BLOB_READ_WRITE_TOKEN in AI Studio Secrets."
                )
            )
        Result.success(requests.map { (path, _) -> UploadTargetToken(pathname = path, token = rwToken) })
    }

    suspend fun uploadPickedMediaUri(
        context: Context,
        uri: Uri,
        isAdmin: Boolean,
        db: FirebaseFirestore = getDb(context),
        auth: FirebaseAuth = Firebase.auth,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required before uploading media."))

        val isVideo = MediaUtils.isVideoContentUri(context, uri)
        return if (isVideo) {
            uploadVideoUri(context, uri, uid, isAdmin, db, auth, onProgress)
        } else {
            uploadImageUri(context, uri, uid, isAdmin, db, auth, onProgress)
        }
    }

    suspend fun uploadStagedStoryFile(
        context: Context,
        file: File,
        isVideo: Boolean,
        isAdmin: Boolean,
        db: FirebaseFirestore = getDb(context),
        auth: FirebaseAuth = Firebase.auth,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = uploadStagedMediaFile(
        context = context,
        file = file,
        isVideo = isVideo,
        isAdmin = isAdmin,
        db = db,
        auth = auth,
        onProgress = onProgress
    )

    private data class PreparedStagedUpload(
        val index: Int,
        val isVideo: Boolean,
        val fileUri: Uri,
        val desiredPath: String,
        val mimeType: String,
        val totalBytes: Long,
        val compressedBytes: ByteArray?,
        val videoFile: File?
    )

    suspend fun uploadStagedMediaBatch(
        context: Context,
        stagedItems: List<Pair<File, Boolean>>,
        auth: FirebaseAuth = Firebase.auth,
        onProgress: (Float) -> Unit = {}
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (stagedItems.isEmpty()) return@withContext Result.success(emptyList())
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(PermanentMediaException("Sign in required before uploading media."))

        onProgress(0.04f)
        val prepared = try {
            coroutineScope {
                stagedItems.mapIndexed { index, (file, isVideo) ->
                    async(Dispatchers.IO) {
                        if (!file.exists()) {
                            throw IllegalArgumentException("Selected media file is missing.")
                        }
                        val fileUri = Uri.fromFile(file)
                        if (isVideo) {
                            val totalBytes = file.length()
                            if (totalBytes <= 0L) {
                                throw IllegalArgumentException("Selected video file is empty.")
                            }
                            PreparedStagedUpload(
                                index = index,
                                isVideo = true,
                                fileUri = fileUri,
                                desiredPath = "campus/$uid/videos/vid_${UUID.randomUUID()}.mp4",
                                mimeType = "video/mp4",
                                totalBytes = totalBytes,
                                compressedBytes = null,
                                videoFile = file
                            )
                        } else {
                            val bytes = MediaUtils.compressUriToJpegBytes(
                                context = context,
                                uri = fileUri,
                                maxDimension = 1600,
                                quality = 86
                            ) ?: throw IllegalArgumentException("Unable to read selected photo.")
                            PreparedStagedUpload(
                                index = index,
                                isVideo = false,
                                fileUri = fileUri,
                                desiredPath = "campus/$uid/images/img_${UUID.randomUUID()}.jpg",
                                mimeType = "image/jpeg",
                                totalBytes = bytes.size.toLong(),
                                compressedBytes = bytes,
                                videoFile = null
                            )
                        }
                    }
                }.awaitAll()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            return@withContext Result.failure(t)
        }

        onProgress(0.14f)
        val targets = resolveUploadTargets(
            auth = auth,
            requests = prepared.map { it.desiredPath to it.mimeType }
        ).getOrElse { return@withContext Result.failure(it) }

        onProgress(0.18f)
        val itemProgress = FloatArray(prepared.size)
        val progressLock = Any()
        val semaphore = Semaphore(3)

        val uploadedUrls = try {
            coroutineScope {
                prepared.mapIndexed { idx, item ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val target = targets[idx]
                            val uploadRes = putStreamToVercelBlob(
                                token = target.token,
                                fileName = target.pathname,
                                mimeType = item.mimeType,
                                totalBytes = item.totalBytes,
                                openStream = {
                                    if (item.compressedBytes != null) {
                                        item.compressedBytes.inputStream()
                                    } else {
                                        BufferedInputStream(FileInputStream(item.videoFile!!), 65_536)
                                    }
                                },
                                onProgress = { pct ->
                                    synchronized(progressLock) {
                                        itemProgress[idx] = pct.coerceIn(0f, 1f)
                                        val avg = itemProgress.sum() / itemProgress.size.toFloat()
                                        onProgress((0.18f + avg * 0.80f).coerceIn(0.18f, 0.98f))
                                    }
                                }
                            )
                            val rawUrl = uploadRes.getOrThrow()
                            if (item.isVideo) {
                                val finalUrl = if (MediaUtils.isVideoMediaUri(rawUrl)) {
                                    rawUrl
                                } else {
                                    "$rawUrl${MediaUtils.VIDEO_URL_FRAGMENT}"
                                }
                                val thumbBytes = MediaUtils.extractVideoThumbnailDataUrl(context, item.fileUri)
                                    ?.let { MediaUtils.decodeDataUriBytes(it) }
                                if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                                    val key = sha256Hex(finalUrl)
                                    imageMemoryCache.put(key, thumbBytes)
                                    runCatching { File(mediaCacheDir(context), "${key}_vthumb.jpg").writeBytes(thumbBytes) }
                                }
                                finalUrl
                            } else {
                                item.compressedBytes?.let { bytes ->
                                    val key = sha256Hex(rawUrl)
                                    imageMemoryCache.put(key, bytes)
                                    runCatching { File(mediaCacheDir(context), "$key.jpg").writeBytes(bytes) }
                                }
                                rawUrl
                            }
                        }
                    }
                }.awaitAll()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            return@withContext Result.failure(t)
        }

        onProgress(1f)
        Result.success(uploadedUrls)
    }

    suspend fun uploadStagedMediaFile(
        context: Context,
        file: File,
        isVideo: Boolean,
        isAdmin: Boolean,
        db: FirebaseFirestore = getDb(context),
        auth: FirebaseAuth = Firebase.auth,
        onProgress: (Float) -> Unit = {}
    ): Result<String> =
        uploadStagedMediaBatch(
            context = context,
            stagedItems = listOf(file to isVideo),
            auth = auth,
            onProgress = onProgress
        ).map { urls ->
            urls.firstOrNull() ?: throw IllegalStateException("Upload returned empty URL.")
        }

    private suspend fun uploadImageUri(
        context: Context,
        uri: Uri,
        uid: String,
        isAdmin: Boolean,
        db: FirebaseFirestore,
        auth: FirebaseAuth,
        onProgress: (Float) -> Unit
    ): Result<String> {
        onProgress(0.15f)
        val bytes = MediaUtils.compressUriToJpegBytes(
            context = context,
            uri = uri,
            maxDimension = 1600,
            quality = 86
        ) ?: return Result.failure(IllegalArgumentException("Unable to read selected photo."))
        onProgress(0.25f)

        val desiredPath = "campus/$uid/images/img_${UUID.randomUUID()}.jpg"
        val target = resolveUploadTargets(auth, listOf(desiredPath to "image/jpeg"))
            .getOrElse { return Result.failure(it) }
            .first()
        onProgress(0.32f)

        val res = putStreamToVercelBlob(
            token = target.token,
            fileName = target.pathname,
            mimeType = "image/jpeg",
            totalBytes = bytes.size.toLong(),
            openStream = { bytes.inputStream() },
            onProgress = { pct -> onProgress((0.32f + pct * 0.68f).coerceIn(0.32f, 1f)) }
        )
        res.getOrNull()?.let { uploadedUrl ->
            val key = sha256Hex(uploadedUrl)
            imageMemoryCache.put(key, bytes)
            runCatching { File(mediaCacheDir(context), "$key.jpg").writeBytes(bytes) }
        }
        return res
    }

    private suspend fun uploadVideoUri(
        context: Context,
        uri: Uri,
        uid: String,
        isAdmin: Boolean,
        db: FirebaseFirestore,
        auth: FirebaseAuth,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        onProgress(0.03f)
        val mimeType = context.contentResolver.getType(uri)?.take(80) ?: "video/mp4"
        val ext = when {
            mimeType.contains("webm") -> "webm"
            mimeType.contains("quicktime") || mimeType.contains("mov") -> "mov"
            mimeType.contains("x-matroska") || mimeType.contains("mkv") -> "mkv"
            else -> "mp4"
        }
        val desiredPath = "campus/$uid/videos/vid_${UUID.randomUUID()}.$ext"
        val target = resolveUploadTargets(auth, listOf(desiredPath to mimeType))
            .getOrElse { return@withContext Result.failure(it) }
            .first()
        val knownSize = queryUriByteSize(context, uri)
        var tempStagedFile: File? = null
        try {
            val streamSize: Long
            val opener: () -> InputStream?
            if (knownSize > 0L) {
                streamSize = knownSize
                opener = { context.contentResolver.openInputStream(uri) }
            } else {
                val tmp = File(mediaCacheDir(context), "upload_tmp_${UUID.randomUUID()}.$ext")
                tempStagedFile = tmp
                val copied = context.contentResolver.openInputStream(uri)?.use { src ->
                    FileOutputStream(tmp).use { out -> src.copyTo(out, 65_536) }
                } ?: 0L
                if (copied <= 0L) {
                    return@withContext Result.failure(IllegalArgumentException("Selected video file is empty or unreadable."))
                }
                streamSize = copied
                opener = { BufferedInputStream(FileInputStream(tmp), 65_536) }
            }

            val result = putStreamToVercelBlob(
                token = target.token,
                fileName = target.pathname,
                mimeType = mimeType,
                totalBytes = streamSize,
                openStream = opener,
                onProgress = onProgress
            ).map { rawUrl ->
                if (MediaUtils.isVideoMediaUri(rawUrl)) rawUrl else "$rawUrl${MediaUtils.VIDEO_URL_FRAGMENT}"
            }
            result.getOrNull()?.let { uploadedUrl ->
                val thumbBytes = MediaUtils.extractVideoThumbnailDataUrl(context, uri)
                    ?.let { MediaUtils.decodeDataUriBytes(it) }
                if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                    val key = sha256Hex(uploadedUrl)
                    imageMemoryCache.put(key, thumbBytes)
                    runCatching { File(mediaCacheDir(context), "${key}_vthumb.jpg").writeBytes(thumbBytes) }
                }
            }
            result
        } finally {
            runCatching { tempStagedFile?.delete() }
        }
    }

    private fun queryUriByteSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            val len = File(uri.path!!).length()
            if (len > 0L) return len
        }
        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 0L) return afd.length
            }
        }
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) {
                    val sz = cursor.getLong(idx)
                    if (sz > 0L) return sz
                }
            }
        }
        return -1L
    }

    private suspend fun putStreamToVercelBlob(
        token: String,
        fileName: String,
        mimeType: String,
        totalBytes: Long,
        openStream: () -> InputStream?,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encodedPath = fileName.split("/")
                .filter { it.isNotBlank() }
                .joinToString("/") { Uri.encode(it) }
            val url = URL("$VERCEL_BLOB_BASE_URL$encodedPath")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 30_000
                readTimeout = 300_000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("x-api-version", "7")
                setRequestProperty("x-vercel-blob-access", "public")
                setRequestProperty("x-content-type", mimeType)
                if (!token.startsWith("vercel_blob_client_")) {
                    setRequestProperty("x-add-random-suffix", "1")
                }
                setRequestProperty("Content-Type", mimeType)
                if (totalBytes > 0L) {
                    setFixedLengthStreamingMode(totalBytes)
                } else {
                    setChunkedStreamingMode(65_536)
                }
            }

            onProgress(0.05f)
            val input = openStream()
                ?: return@withContext Result.failure(IllegalStateException("Unable to read selected media stream."))
            input.use { src ->
                BufferedOutputStream(conn.outputStream, 65_536).use { out ->
                    val buffer = ByteArray(65_536)
                    var written = 0L
                    var lastReportedPct = -1
                    while (true) {
                        val read = src.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        written += read
                        if (totalBytes > 0L) {
                            val fraction = (written.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0)
                            val pct = (fraction * 90.0).toInt()
                            if (pct > lastReportedPct) {
                                lastReportedPct = pct
                                onProgress((0.05f + (fraction * 0.90).toFloat()).coerceIn(0.05f, 0.95f))
                            }
                        }
                    }
                    out.flush()
                }
            }

            val code = conn.responseCode
            val responseBody = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")

            if (code !in 200..299) {
                val parsed = runCatching {
                    val json = JSONObject(responseBody)
                    json.optJSONObject("error")?.optString("message").orEmpty()
                        .ifBlank { json.optString("error") }
                        .ifBlank { json.optString("message") }
                }.getOrDefault("").trim()
                val errMsg = when {
                    parsed.isBlank() -> "Upload failed (HTTP $code)"
                    parsed.contains(code.toString()) -> parsed
                    else -> "$parsed (HTTP $code)"
                }
                Log.e("CampusMediaRepository", "Vercel Blob PUT failed: $errMsg")
                val err = if (code in 400..499 && code != 408 && code != 429) {
                    PermanentMediaException(errMsg)
                } else {
                    IllegalStateException(errMsg)
                }
                return@withContext Result.failure(err)
            }

            val blobUrl = runCatching { JSONObject(responseBody).optString("url") }.getOrDefault("").trim()
            if (blobUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Vercel Blob server did not return a valid URL."))
            }
            onProgress(1f)
            Result.success(blobUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(if (t is Exception) t else IllegalStateException(t.message ?: "Media upload failed", t))
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private suspend fun uploadChunkedFile(
        file: File,
        totalBytes: Long,
        mimeType: String,
        uriPrefix: String,
        thumbnailData: String?,
        uid: String,
        db: FirebaseFirestore,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val assetId = UUID.randomUUID().toString()
        val assetRef = db.collection("media_assets").document(assetId)
        var createdManifest = false
        try {
            val chunkSize = MediaUtils.CHUNK_RAW_BYTES
            val totalChunks = ((totalBytes + chunkSize - 1L) / chunkSize.toLong()).toInt().coerceAtLeast(1)

            val manifest = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "mimeType" to mimeType,
                "chunkCount" to totalChunks,
                "totalBytes" to totalBytes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!thumbnailData.isNullOrBlank()) {
                manifest["thumbnailData"] = thumbnailData
            }
            assetRef.set(manifest).await()
            createdManifest = true
            onProgress(0.15f)

            val completedChunks = AtomicInteger(0)
            BufferedInputStream(FileInputStream(file), 65_536).use { bis ->
                val readBuf = ByteArray(chunkSize)
                var nextIndex = 0
                while (true) {
                    val batch = ArrayList<Pair<Int, String>>(3)
                    for (slot in 0 until 3) {
                        var filled = 0
                        while (filled < chunkSize) {
                            val r = bis.read(readBuf, filled, chunkSize - filled)
                            if (r < 0) break
                            filled += r
                        }
                        if (filled <= 0) break
                        val b64 = Base64.encodeToString(readBuf, 0, filled, Base64.NO_WRAP)
                        batch.add(nextIndex to b64)
                        nextIndex++
                    }
                    if (batch.isEmpty()) break
                    coroutineScope {
                        batch.map { (chunkIdx, b64) ->
                            async {
                                assetRef.collection("chunks").document("chunk_$chunkIdx").set(
                                    mapOf(
                                        "ownerId" to uid,
                                        "index" to chunkIdx,
                                        "data" to b64
                                    )
                                ).await()
                                val done = completedChunks.incrementAndGet()
                                onProgress(0.15f + 0.85f * (done.toFloat() / totalChunks.toFloat()).coerceIn(0f, 1f))
                            }
                        }.awaitAll()
                    }
                }
            }
            Result.success("$uriPrefix$assetId")
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (createdManifest) {
                runCatching { deleteSingleMediaAssetDoc(assetId, db) }
            }
            Result.failure(if (t is Exception) t else IllegalStateException(t.message ?: "Video upload failed", t))
        }
    }

    private suspend fun uploadChunkedBytes(
        bytes: ByteArray,
        mimeType: String,
        uriPrefix: String,
        thumbnailData: String?,
        uid: String,
        db: FirebaseFirestore,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val assetId = UUID.randomUUID().toString()
        val assetRef = db.collection("media_assets").document(assetId)
        var createdManifest = false
        return try {
            val chunkSize = MediaUtils.CHUNK_RAW_BYTES
            val totalChunks = ((bytes.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)

            val manifest = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "mimeType" to mimeType,
                "chunkCount" to totalChunks,
                "totalBytes" to bytes.size,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!thumbnailData.isNullOrBlank()) {
                manifest["thumbnailData"] = thumbnailData
            }
            assetRef.set(manifest).await()
            createdManifest = true
            onProgress(0.15f)

            for (index in 0 until totalChunks) {
                val start = index * chunkSize
                val end = minOf(start + chunkSize, bytes.size)
                val b64 = Base64.encodeToString(bytes, start, end - start, Base64.NO_WRAP)
                assetRef.collection("chunks").document("chunk_$index").set(
                    mapOf(
                        "ownerId" to uid,
                        "index" to index,
                        "data" to b64
                    )
                ).await()
                onProgress(0.15f + 0.85f * ((index + 1).toFloat() / totalChunks.toFloat()))
            }
            Result.success("$uriPrefix$assetId")
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (createdManifest) {
                runCatching { deleteSingleMediaAssetDoc(assetId, db) }
            }
            Result.failure(if (t is Exception) t else IllegalStateException(t.message ?: "Upload failed", t))
        }
    }

    suspend fun deleteMediaAssetsForUris(
        uris: Collection<String>,
        isAdmin: Boolean,
        db: FirebaseFirestore,
        auth: FirebaseAuth,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val assetIds = uris.mapNotNull { MediaUtils.extractAssetId(it) }.distinct()
        for (assetId in assetIds) {
            CampusChunkedVideoCache.evictAsset(context, assetId)
            imageMemoryCache.remove(assetId)
            if (context != null) {
                val dir = mediaCacheDir(context)
                runCatching { File(dir, "$assetId.mp4").delete() }
                runCatching { File(dir, "$assetId.jpg").delete() }
                runCatching { File(dir, "${assetId}_thumb.jpg").delete() }
            }
            runCatching {
                val snap = db.collection("media_assets").document(assetId).get().await()
                if (snap.exists() && (isAdmin || snap.getString("ownerId") == uid)) {
                    deleteSingleMediaAssetDoc(assetId, db)
                }
            }
        }
    }

    private suspend fun deleteSingleMediaAssetDoc(assetId: String, db: FirebaseFirestore) {
        val assetRef = db.collection("media_assets").document(assetId)
        val chunksSnap = assetRef.collection("chunks").get().await()
        if (!chunksSnap.isEmpty) {
            chunksSnap.documents.chunked(400).forEach { group ->
                val batch = db.batch()
                group.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }
        assetRef.delete().await()
    }

    suspend fun resolveMediaImageBytes(
        context: Context,
        mediaUri: String,
        db: FirebaseFirestore = getDb(context)
    ): ByteArray? = withContext(Dispatchers.IO) {
        val assetId = MediaUtils.extractAssetId(mediaUri) ?: return@withContext null
        imageMemoryCache.get(assetId)?.let { return@withContext it }

        val ext = if (MediaUtils.isVideoMediaUri(mediaUri)) "thumb.jpg" else "jpg"
        val cachedFile = File(mediaCacheDir(context), "${assetId}_$ext")
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            return@withContext cachedFile.readBytes().also { imageMemoryCache.put(assetId, it) }
        }

        try {
            val assetSnap = db.collection("media_assets").document(assetId).get().await()
            val thumb = assetSnap.getString("thumbnailData")
            val thumbBytes = MediaUtils.decodeDataUriBytes(thumb)
            if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                cachedFile.writeBytes(thumbBytes)
                imageMemoryCache.put(assetId, thumbBytes)
                return@withContext thumbBytes
            }

            if (!MediaUtils.isVideoMediaUri(mediaUri)) {
                val assembled = downloadChunksBytes(assetId, db)
                if (assembled != null && assembled.isNotEmpty()) {
                    cachedFile.writeBytes(assembled)
                    imageMemoryCache.put(assetId, assembled)
                    return@withContext assembled
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resolveRemoteVideoThumbnailBytes(
        context: Context,
        videoUrl: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (!MediaUtils.isRemoteHttpUri(videoUrl)) return@withContext null
        val key = sha256Hex(videoUrl)
        imageMemoryCache.get(key)?.let { return@withContext it }

        val cachedFile = File(mediaCacheDir(context), "${key}_vthumb.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            return@withContext cachedFile.readBytes().also { imageMemoryCache.put(key, it) }
        }

        val retriever = MediaMetadataRetriever()
        try {
            val cleanUrl = videoUrl.substringBefore('#')
            retriever.setDataSource(cleanUrl, emptyMap())
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return@withContext null
            val maxDim = 480
            val srcMax = max(frame.width, frame.height)
            val thumb = if (srcMax > maxDim && srcMax > 0) {
                val scale = maxDim.toFloat() / srcMax.toFloat()
                Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).roundToInt().coerceAtLeast(1),
                    (frame.height * scale).roundToInt().coerceAtLeast(1),
                    true
                ).also { if (it != frame) frame.recycle() }
            } else frame
            val baos = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, 76, baos)
            thumb.recycle()
            val bytes = baos.toByteArray()
            if (bytes.isNotEmpty()) {
                runCatching { cachedFile.writeBytes(bytes) }
                imageMemoryCache.put(key, bytes)
                bytes
            } else null
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun resolveNaturalAspectRatio(
        context: Context,
        mediaUri: String?
    ): Float? = withContext(Dispatchers.IO) {
        val raw = mediaUri?.trim()?.takeIf { it.isNotBlank() } ?: return@withContext null
        naturalAspectRatioCache[raw]?.let { return@withContext it }

        val bytes: ByteArray? = when {
            raw.startsWith("data:", ignoreCase = true) -> MediaUtils.decodeDataUriBytes(raw)
            MediaUtils.isChunkedMediaUri(raw) -> resolveMediaImageBytes(context, raw)
            MediaUtils.isRemoteHttpUri(raw) -> {
                if (MediaUtils.isVideoMediaUri(raw)) {
                    resolveRemoteVideoThumbnailBytes(context, raw)
                } else {
                    val key = sha256Hex(raw)
                    imageMemoryCache.get(key) ?: run {
                        val cachedFile = File(mediaCacheDir(context), "$key.jpg")
                        if (cachedFile.exists() && cachedFile.length() > 0L) {
                            cachedFile.readBytes().also { imageMemoryCache.put(key, it) }
                        } else {
                            runCatching {
                                val conn = (URL(raw).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 10_000
                                    readTimeout = 15_000
                                    instanceFollowRedirects = true
                                }
                                conn.inputStream.use { it.readBytes() }
                            }.getOrNull()?.also { fetched ->
                                if (fetched.isNotEmpty()) {
                                    imageMemoryCache.put(key, fetched)
                                    runCatching { cachedFile.writeBytes(fetched) }
                                }
                            }
                        }
                    }
                }
            }
            else -> null
        }

        if (bytes != null && bytes.isNotEmpty()) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                val ratio = (opts.outWidth.toFloat() / opts.outHeight.toFloat()).coerceIn(0.45f, 2.1f)
                naturalAspectRatioCache[raw] = ratio
                return@withContext ratio
            }
        }
        null
    }

    suspend fun resolveVideoFile(
        context: Context,
        videoUri: String,
        db: FirebaseFirestore = getDb(context)
    ): Result<File> = withContext(Dispatchers.IO) {
        val assetId = MediaUtils.extractAssetId(videoUri)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid video URI"))

        val targetFile = File(mediaCacheDir(context), "$assetId.mp4")
        if (targetFile.exists() && targetFile.length() > 0L) {
            runCatching { targetFile.setLastModified(System.currentTimeMillis()) }
            return@withContext Result.success(targetFile)
        }

        return@withContext try {
            val snap = db.collection("media_assets").document(assetId)
                .collection("chunks")
                .orderBy("index")
                .get()
                .await()
            if (snap.isEmpty) {
                return@withContext Result.failure(IllegalStateException("Video chunks missing."))
            }
            val tmp = File(mediaCacheDir(context), "$assetId.tmp")
            FileOutputStream(tmp).use { out ->
                for (doc in snap.documents) {
                    val data = doc.getString("data") ?: continue
                    val decoded = Base64.decode(data, Base64.DEFAULT)
                    if (decoded.isNotEmpty()) out.write(decoded)
                }
                out.flush()
            }
            if (targetFile.exists()) targetFile.delete()
            tmp.renameTo(targetFile)
            targetFile.setLastModified(System.currentTimeMillis())
            Result.success(targetFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadChunksBytes(assetId: String, db: FirebaseFirestore): ByteArray? {
        val snap = db.collection("media_assets").document(assetId)
            .collection("chunks")
            .orderBy("index")
            .get()
            .await()
        if (snap.isEmpty) return null
        val baos = ByteArrayOutputStream()
        for (doc in snap.documents) {
            val data = doc.getString("data") ?: continue
            val decoded = Base64.decode(data, Base64.DEFAULT)
            baos.write(decoded)
        }
        return baos.toByteArray()
    }

    suspend fun resolveFullImageBytesForSave(
        context: Context,
        mediaUriOrDataUri: String,
        db: FirebaseFirestore = getDb(context)
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (mediaUriOrDataUri.isBlank() || MediaUtils.isVideoMediaUri(mediaUriOrDataUri)) {
            return@withContext null
        }
        if (mediaUriOrDataUri.startsWith("data:", ignoreCase = true)) {
            return@withContext MediaUtils.decodeDataUriBytes(mediaUriOrDataUri)
        }
        val assetId = MediaUtils.extractAssetId(mediaUriOrDataUri)
        if (assetId != null) {
            val fullCached = File(mediaCacheDir(context), "$assetId.jpg")
            if (fullCached.exists() && fullCached.length() > 0L) {
                return@withContext fullCached.readBytes()
            }
            val assembled = runCatching { downloadChunksBytes(assetId, db) }.getOrNull()
            if (assembled != null && assembled.isNotEmpty()) {
                runCatching { fullCached.writeBytes(assembled) }
                imageMemoryCache.put(assetId, assembled)
                return@withContext assembled
            }
            return@withContext resolveMediaImageBytes(context, mediaUriOrDataUri, db)
        }
        if (mediaUriOrDataUri.startsWith("http://", ignoreCase = true) ||
            mediaUriOrDataUri.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext runCatching {
                val conn = (URL(mediaUriOrDataUri).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { it.readBytes() }
            }.getOrNull()
        }
        null
    }

    suspend fun saveImageToGallery(
        context: Context,
        mediaUriOrDataUri: String,
        suggestedName: String = "Squadify_${System.currentTimeMillis()}.jpg"
    ): Result<String> = withContext(Dispatchers.IO) {
        val bytes = resolveFullImageBytesForSave(context, mediaUriOrDataUri)
            ?: return@withContext Result.failure(IllegalStateException("Unable to load photo for saving."))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext Result.failure(IllegalStateException("SAF required below Android 10"))
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, suggestedName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Squadify")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore entry."))
        return@withContext try {
            resolver.openOutputStream(itemUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: throw IllegalStateException("Could not open output stream.")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            Result.success("Saved to Pictures/Squadify")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { resolver.delete(itemUri, null, null) }
            Result.failure(e)
        }
    }

    suspend fun writeImageToSafUri(
        context: Context,
        targetUri: Uri,
        mediaUriOrDataUri: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val bytes = resolveFullImageBytesForSave(context, mediaUriOrDataUri)
            ?: return@withContext Result.failure(IllegalStateException("Unable to load photo for saving."))
        return@withContext try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: throw IllegalStateException("Could not open selected destination.")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
