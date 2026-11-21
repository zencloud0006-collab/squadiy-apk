package com.bsdcampus.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.LruCache
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache
import java.util.concurrent.ConcurrentHashMap
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object CampusMediaRepository {
    private val imageMemoryCache = LruCache<String, ByteArray>(24)
    private val naturalAspectRatioCache = ConcurrentHashMap<String, Float>()

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
            uploadVideoUri(context, uri, uid, isAdmin, db, onProgress)
        } else {
            uploadImageUri(context, uri, uid, isAdmin, db, onProgress)
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

    suspend fun uploadStagedMediaFile(
        context: Context,
        file: File,
        isVideo: Boolean,
        isAdmin: Boolean,
        db: FirebaseFirestore = getDb(context),
        auth: FirebaseAuth = Firebase.auth,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required before uploading media."))
        val fileUri = Uri.fromFile(file)
        return if (isVideo) {
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("Selected video file is missing."))
            }
            val totalBytes = file.length()
            if (totalBytes <= 0L) {
                return Result.failure(IllegalArgumentException("Selected video file is empty."))
            }
            if (!isAdmin && totalBytes > MediaUtils.MAX_REGULAR_USER_VIDEO_BYTES) {
                return Result.failure(IllegalArgumentException("Video exceeds the 25 MB limit."))
            }
            onProgress(0.05f)
            val thumbData = MediaUtils.extractVideoThumbnailDataUrl(context, fileUri)
            onProgress(0.10f)
            uploadChunkedFile(
                file = file,
                totalBytes = totalBytes,
                mimeType = "video/mp4",
                uriPrefix = MediaUtils.VIDEO_URI_PREFIX,
                thumbnailData = thumbData,
                uid = uid,
                db = db,
                onProgress = onProgress
            ).also { res ->
                res.getOrNull()?.let { uriStr ->
                    MediaUtils.extractAssetId(uriStr)?.let { id ->
                        runCatching {
                            val target = File(mediaCacheDir(context), "$id.mp4")
                            val tmp = File(mediaCacheDir(context), "$id.tmp")
                            FileInputStream(file).use { input ->
                                FileOutputStream(tmp).use { output ->
                                    input.copyTo(output, bufferSize = 32_768)
                                }
                            }
                            if (target.exists()) target.delete()
                            tmp.renameTo(target)
                        }
                    }
                }
            }
        } else {
            uploadImageUri(context, fileUri, uid, isAdmin, db, onProgress)
        }
    }

    private suspend fun uploadImageUri(
        context: Context,
        uri: Uri,
        uid: String,
        isAdmin: Boolean,
        db: FirebaseFirestore,
        onProgress: (Float) -> Unit
    ): Result<String> {
        onProgress(0.2f)
        // For admins we store pointer URIs (bsdmedia://<id>) so a single post can hold unlimited photos
        // without exceeding Firestore's 1 MiB document size cap.
        if (!isAdmin) {
            val inline = MediaUtils.compressUriToDataUrl(context, uri)
            if (inline != null) {
                onProgress(1f)
                return Result.success(inline)
            }
        }
        val bytes = MediaUtils.compressUriToJpegBytes(
            context = context,
            uri = uri,
            maxDimension = if (isAdmin) 1440 else 1080,
            quality = if (isAdmin) 84 else 76
        ) ?: return Result.failure(IllegalArgumentException("Unable to read selected photo."))

        val thumbDataUrl = MediaUtils.compressUriToDataUrl(context, uri, maxDimension = 420)
        return uploadChunkedBytes(
            bytes = bytes,
            mimeType = "image/jpeg",
            uriPrefix = MediaUtils.IMAGE_ASSET_URI_PREFIX,
            thumbnailData = thumbDataUrl,
            uid = uid,
            db = db,
            onProgress = onProgress
        ).also { res ->
            res.getOrNull()?.let { uriStr ->
                MediaUtils.extractAssetId(uriStr)?.let { id ->
                    imageMemoryCache.put(id, bytes)
                    runCatching {
                        File(mediaCacheDir(context), "$id.jpg").writeBytes(bytes)
                    }
                }
            }
        }
    }

    private suspend fun uploadVideoUri(
        context: Context,
        uri: Uri,
        uid: String,
        isAdmin: Boolean,
        db: FirebaseFirestore,
        onProgress: (Float) -> Unit
    ): Result<String> {
        onProgress(0.1f)
        val maxBytes = if (isAdmin) null else MediaUtils.MAX_REGULAR_USER_VIDEO_BYTES
        val bytes = MediaUtils.readUriBytesWithLimit(context, uri, maxBytes)
            ?: return Result.failure(
                IllegalArgumentException(
                    if (isAdmin) "Unable to read selected video."
                    else "Video exceeds the 25 MB limit for student/teacher posts."
                )
            )
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Selected video file is empty."))
        }

        val mimeType = context.contentResolver.getType(uri)?.take(80) ?: "video/mp4"
        val thumbData = MediaUtils.extractVideoThumbnailDataUrl(context, uri)
        return uploadChunkedBytes(
            bytes = bytes,
            mimeType = mimeType,
            uriPrefix = MediaUtils.VIDEO_URI_PREFIX,
            thumbnailData = thumbData,
            uid = uid,
            db = db,
            onProgress = onProgress
        ).also { res ->
            res.getOrNull()?.let { uriStr ->
                MediaUtils.extractAssetId(uriStr)?.let { id ->
                    runCatching {
                        File(mediaCacheDir(context), "$id.mp4").writeBytes(bytes)
                    }
                }
            }
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

    suspend fun resolveNaturalAspectRatio(
        context: Context,
        mediaUri: String?
    ): Float? = withContext(Dispatchers.IO) {
        val raw = mediaUri?.trim()?.takeIf { it.isNotBlank() } ?: return@withContext null
        naturalAspectRatioCache[raw]?.let { return@withContext it }

        val bytes: ByteArray? = when {
            raw.startsWith("data:", ignoreCase = true) -> MediaUtils.decodeDataUriBytes(raw)
            MediaUtils.isChunkedMediaUri(raw) -> resolveMediaImageBytes(context, raw)
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
