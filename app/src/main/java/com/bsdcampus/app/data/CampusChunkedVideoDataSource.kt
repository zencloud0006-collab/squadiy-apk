package com.bsdcampus.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.bsdcampus.app.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * High-speed progressive Media3 cache & DataSource for `bsdvideo://` / `bsdmedia://` assets.
 * Fetches only the manifest + requested chunks on demand (e.g. chunk 0 + trailing moov chunk),
 * prefetches ahead concurrently, and persists the assembled `.mp4` once all chunks finish.
 */
object CampusChunkedVideoCache {
    private data class VideoManifest(
        val chunkCount: Int,
        val totalBytes: Long,
        val chunkSize: Int
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manifests = ConcurrentHashMap<String, VideoManifest>()
    private val manifestInFlight = ConcurrentHashMap<String, Deferred<VideoManifest?>>()
    private val chunkInFlight = ConcurrentHashMap<String, Deferred<ByteArray?>>()
    private val legacyFallbackLocks = ConcurrentHashMap<String, Deferred<File?>>()

    // ~24 MB in-memory chunk cache (~50 chunks) for zero-latency scrubbing/replaying
    private val chunkMemoryCache = object : LruCache<String, ByteArray>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    private fun cacheDir(context: Context): File =
        File(context.applicationContext.cacheDir, "campus_media").apply { mkdirs() }

    fun cachedMp4File(context: Context, assetId: String): File? {
        val file = File(cacheDir(context), "$assetId.mp4")
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun evictAsset(context: Context?, assetId: String) {
        manifests.remove(assetId)
        val prefix = "$assetId:"
        chunkMemoryCache.snapshot().keys.filter { it.startsWith(prefix) }.forEach {
            chunkMemoryCache.remove(it)
        }
        if (context != null) {
            val dir = cacheDir(context)
            runCatching { File(dir, "$assetId.mp4").delete() }
            runCatching { File(dir, "$assetId.tmp").delete() }
        }
    }

    private fun chunkCacheKey(assetId: String, chunkIdx: Int): String = "$assetId:$chunkIdx"

    private suspend fun getManifestAndZero(
        assetId: String,
        db: FirebaseFirestore
    ): VideoManifest? {
        manifests[assetId]?.let { return it }
        val deferred = manifestInFlight.computeIfAbsent(assetId) {
            scope.async {
                try {
                    val assetRef = db.collection("media_assets").document(assetId)
                    val assetDeferred = async { assetRef.get().await() }
                    val chunk0Deferred = async {
                        assetRef.collection("chunks").document("chunk_0").get().await()
                    }
                    val assetSnap = assetDeferred.await()
                    val chunk0Snap = chunk0Deferred.await()

                    val totalBytes = assetSnap.getLong("totalBytes") ?: 0L
                    val chunkCount = (assetSnap.getLong("chunkCount") ?: 0L).toInt()
                    val chunk0B64 = chunk0Snap.getString("data")
                    if (totalBytes <= 0L || chunkCount <= 0 || chunk0B64.isNullOrBlank()) {
                        return@async null
                    }
                    val chunk0Bytes = Base64.decode(chunk0B64, Base64.DEFAULT)
                    if (chunk0Bytes.isEmpty()) return@async null

                    chunkMemoryCache.put(chunkCacheKey(assetId, 0), chunk0Bytes)
                    val chunkSize = chunk0Bytes.size
                    val uniformLayoutValid = if (chunkCount == 1) {
                        chunkSize.toLong() == totalBytes
                    } else {
                        val minPrev = chunkSize.toLong() * (chunkCount - 1).toLong()
                        val maxTotal = chunkSize.toLong() * chunkCount.toLong()
                        totalBytes in (minPrev + 1L)..maxTotal
                    }
                    if (!uniformLayoutValid) return@async null

                    VideoManifest(
                        chunkCount = chunkCount,
                        totalBytes = totalBytes,
                        chunkSize = chunkSize
                    ).also { manifests[assetId] = it }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                } finally {
                    manifestInFlight.remove(assetId)
                }
            }
        }
        return deferred.await()
    }

    private suspend fun fetchChunk(
        assetId: String,
        chunkIdx: Int,
        db: FirebaseFirestore
    ): ByteArray? {
        val key = chunkCacheKey(assetId, chunkIdx)
        chunkMemoryCache.get(key)?.let { return it }

        val deferred = chunkInFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val doc = db.collection("media_assets").document(assetId)
                        .collection("chunks").document("chunk_$chunkIdx")
                        .get()
                        .await()
                    val b64 = doc.getString("data") ?: return@async null
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    if (bytes.isNotEmpty()) {
                        chunkMemoryCache.put(key, bytes)
                    }
                    bytes
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                } finally {
                    chunkInFlight.remove(key)
                }
            }
        }
        return deferred.await()
    }

    private fun triggerReadAhead(
        context: Context,
        assetId: String,
        fromChunkIdx: Int,
        manifest: VideoManifest,
        db: FirebaseFirestore
    ) {
        val maxIdx = minOf(fromChunkIdx + 3, manifest.chunkCount - 1)
        for (idx in (fromChunkIdx + 1)..maxIdx) {
            val key = chunkCacheKey(assetId, idx)
            if (chunkMemoryCache.get(key) == null && !chunkInFlight.containsKey(key)) {
                scope.launch {
                    fetchChunk(assetId, idx, db)
                    tryAssembleCompletedFile(context, assetId, manifest)
                }
            }
        }
    }

    private fun tryAssembleCompletedFile(
        context: Context,
        assetId: String,
        manifest: VideoManifest
    ) {
        val target = File(cacheDir(context), "$assetId.mp4")
        if (target.exists() && target.length() == manifest.totalBytes) return
        for (idx in 0 until manifest.chunkCount) {
            if (chunkMemoryCache.get(chunkCacheKey(assetId, idx)) == null) return
        }
        scope.launch {
            if (target.exists() && target.length() == manifest.totalBytes) return@launch
            val tmp = File(cacheDir(context), "$assetId.part.tmp")
            runCatching {
                FileOutputStream(tmp).use { out ->
                    for (idx in 0 until manifest.chunkCount) {
                        val bytes = chunkMemoryCache.get(chunkCacheKey(assetId, idx))
                            ?: throw IllegalStateException("Evicted during assemble")
                        out.write(bytes)
                    }
                    out.flush()
                }
                if (target.exists()) target.delete()
                tmp.renameTo(target)
                target.setLastModified(System.currentTimeMillis())
            }.onFailure {
                runCatching { tmp.delete() }
            }
        }
    }

    /**
     * Pre-warms manifest, first 2 chunks, and the trailing chunk (for MP4 trailing `moov` atoms)
     * in parallel so swiping into a reel starts playback almost immediately.
     */
    suspend fun prefetchVideoHead(
        context: Context,
        videoUri: String,
        db: FirebaseFirestore = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    ) {
        val assetId = MediaUtils.extractAssetId(videoUri) ?: return
        if (cachedMp4File(context, assetId) != null) return
        val manifest = getManifestAndZero(assetId, db) ?: return
        val toWarm = buildList {
            if (manifest.chunkCount > 1) add(1)
            if (manifest.chunkCount > 2) add(manifest.chunkCount - 1)
            if (manifest.chunkCount > 3) add(2)
        }.distinct()
        toWarm.forEach { idx ->
            scope.launch { fetchChunk(assetId, idx, db) }
        }
    }

    internal suspend fun fallbackDownloadFullFile(
        context: Context,
        videoUri: String,
        db: FirebaseFirestore
    ): File? {
        val assetId = MediaUtils.extractAssetId(videoUri) ?: return null
        cachedMp4File(context, assetId)?.let { return it }
        val deferred = legacyFallbackLocks.computeIfAbsent(assetId) {
            scope.async {
                try {
                    CampusMediaRepository.resolveVideoFile(context, videoUri, db).getOrNull()
                } finally {
                    legacyFallbackLocks.remove(assetId)
                }
            }
        }
        return deferred.await()
    }

    @OptIn(UnstableApi::class)
    class Factory(
        private val context: Context,
        private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = CampusChunkedVideoDataSource(context.applicationContext, db)
    }

    @OptIn(UnstableApi::class)
    private class CampusChunkedVideoDataSource(
        private val context: Context,
        private val db: FirebaseFirestore
    ) : BaseDataSource(/* isNetwork = */ true) {
        private var currentDataSpec: DataSpec? = null
        private var currentAssetId: String? = null
        private var currentManifest: VideoManifest? = null
        private var localRaf: RandomAccessFile? = null
        private var position: Long = 0L
        private var bytesRemaining: Long = 0L
        private var opened: Boolean = false

        override fun open(dataSpec: DataSpec): Long {
            val uriStr = dataSpec.uri.toString()
            val assetId = MediaUtils.extractAssetId(uriStr)
                ?: throw IOException("Unsupported chunked video URI: $uriStr")

            currentDataSpec = dataSpec
            currentAssetId = assetId
            transferInitializing(dataSpec)

            val cachedFile = cachedMp4File(context, assetId)
            if (cachedFile != null) {
                return openLocalFile(cachedFile, dataSpec)
            }

            val manifest = runBlocking { getManifestAndZero(assetId, db) }
            if (manifest == null) {
                val fallbackFile = runBlocking { fallbackDownloadFullFile(context, uriStr, db) }
                    ?: throw IOException("Unable to load video stream for $assetId")
                return openLocalFile(fallbackFile, dataSpec)
            }

            currentManifest = manifest
            if (dataSpec.position > manifest.totalBytes) {
                throw IOException("Seek position out of range")
            }
            position = dataSpec.position
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                minOf(dataSpec.length, manifest.totalBytes - position)
            } else {
                manifest.totalBytes - position
            }
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        }

        private fun openLocalFile(file: File, dataSpec: DataSpec): Long {
            val raf = RandomAccessFile(file, "r")
            val totalLen = raf.length()
            if (dataSpec.position > totalLen) {
                raf.close()
                throw IOException("Seek position out of range")
            }
            raf.seek(dataSpec.position)
            localRaf = raf
            position = dataSpec.position
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                minOf(dataSpec.length, totalLen - position)
            } else {
                totalLen - position
            }
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

            val toRead = minOf(length.toLong(), bytesRemaining).toInt()
            localRaf?.let { raf ->
                val read = raf.read(buffer, offset, toRead)
                if (read < 0) return C.RESULT_END_OF_INPUT
                position += read
                bytesRemaining -= read
                bytesTransferred(read)
                return read
            }

            val assetId = currentAssetId ?: return C.RESULT_END_OF_INPUT
            val manifest = currentManifest ?: return C.RESULT_END_OF_INPUT
            val chunkIdx = (position / manifest.chunkSize.toLong()).toInt()
            if (chunkIdx < 0 || chunkIdx >= manifest.chunkCount) {
                return C.RESULT_END_OF_INPUT
            }

            triggerReadAhead(context, assetId, chunkIdx, manifest, db)
            val chunkBytes = runBlocking { fetchChunk(assetId, chunkIdx, db) }
                ?: throw IOException("Missing video chunk $chunkIdx for $assetId")

            val offsetInChunk = (position % manifest.chunkSize.toLong()).toInt()
            val availableInChunk = chunkBytes.size - offsetInChunk
            if (availableInChunk <= 0) return C.RESULT_END_OF_INPUT

            val copyLen = minOf(toRead, availableInChunk)
            System.arraycopy(chunkBytes, offsetInChunk, buffer, offset, copyLen)
            position += copyLen
            bytesRemaining -= copyLen
            bytesTransferred(copyLen)
            tryAssembleCompletedFile(context, assetId, manifest)
            return copyLen
        }

        override fun getUri(): Uri? = currentDataSpec?.uri

        override fun close() {
            currentDataSpec = null
            currentManifest = null
            currentAssetId = null
            try {
                localRaf?.close()
            } finally {
                localRaf = null
                if (opened) {
                    opened = false
                    transferEnded()
                }
            }
        }
    }
}
