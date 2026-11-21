package com.bsdcampus.app.data

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class CacheStats(
    val totalBytes: Long = 0L,
    val mediaCacheBytes: Long = 0L,
    val coilCacheBytes: Long = 0L,
    val tempBytes: Long = 0L,
    val videoFilesCount: Int = 0
)

object CacheManager {
    private const val RECENT_PROTECTION_MS = 10 * 60 * 1000L // 10 min safety window for active playback/recording
    private const val PENDING_STORY_PROTECTION_MS = 12 * 60 * 60 * 1000L // 12h protection for staged uploads

    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe < 1024L -> "$safe B"
            safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            safe < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", safe / (1024.0 * 1024.0 * 1024.0))
        }
    }

    suspend fun measureCache(context: Context): CacheStats = withContext(Dispatchers.IO) {
        val cacheRoot = context.cacheDir
        val mediaDir = File(cacheRoot, "campus_media")
        val musicDir = File(cacheRoot, "campus_music")
        val coilDir = File(cacheRoot, "image_cache")
        val updatesDir = File(cacheRoot, "app_updates")

        var mediaBytes = 0L
        var videoCount = 0
        if (mediaDir.exists()) {
            mediaDir.walkTopDown().filter { it.isFile }.forEach { f ->
                mediaBytes += f.length()
                if (f.name.endsWith(".mp4", ignoreCase = true)) {
                    videoCount++
                }
            }
        }
        if (musicDir.exists()) {
            musicDir.walkTopDown().filter { it.isFile }.forEach { f ->
                mediaBytes += f.length()
            }
        }

        var coilBytes = 0L
        if (coilDir.exists()) {
            coilDir.walkTopDown().filter { it.isFile }.forEach { coilBytes += it.length() }
        }

        var tempBytes = 0L
        if (updatesDir.exists()) {
            updatesDir.walkTopDown().filter { it.isFile }.forEach { tempBytes += fLen(it) }
        }
        cacheRoot.listFiles()?.forEach { file ->
            if (file.isFile && isManagedTempFile(file.name)) {
                tempBytes += file.length()
            }
        }

        CacheStats(
            totalBytes = mediaBytes + coilBytes + tempBytes,
            mediaCacheBytes = mediaBytes,
            coilCacheBytes = coilBytes,
            tempBytes = tempBytes,
            videoFilesCount = videoCount
        )
    }

    suspend fun runAutoCleanup(
        context: Context,
        maxSizeMb: Int = 150,
        maxAgeHours: Int = 48
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cacheRoot = context.cacheDir
        val maxAgeMs = maxAgeHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val maxBytes = maxSizeMb.coerceAtLeast(25) * 1024L * 1024L
        val targetBytes = (maxBytes * 0.80).toLong()
        var freedBytes = 0L

        // 1. Clean expired staged stories and staged post/reel uploads (> 12h old so queued uploads remain safe)
        listOf("pending_stories", "pending_uploads").forEach { stagedFolder ->
            val pendingDir = File(cacheRoot, stagedFolder)
            if (pendingDir.exists()) {
                pendingDir.listFiles()?.forEach { file ->
                    if (file.isFile && now - file.lastModified() > PENDING_STORY_PROTECTION_MS) {
                        val len = file.length()
                        if (file.delete()) freedBytes += len
                    }
                }
            }
        }

        // 2. Clean old downloaded update APKs (> 12h old)
        val updatesDir = File(cacheRoot, "app_updates")
        if (updatesDir.exists()) {
            updatesDir.listFiles()?.forEach { file ->
                if (file.isFile && now - file.lastModified() > PENDING_STORY_PROTECTION_MS) {
                    val len = file.length()
                    if (file.delete()) freedBytes += len
                }
            }
        }

        // 3. Clean stale temp files (voice/edit/tmp)
        cacheRoot.listFiles()?.forEach { file ->
            if (file.isFile && isManagedTempFile(file.name) && now - file.lastModified() > RECENT_PROTECTION_MS) {
                if (file.name.endsWith(".tmp") || now - file.lastModified() > maxAgeMs) {
                    val len = file.length()
                    if (file.delete()) freedBytes += len
                }
            }
        }

        // 4. Clean expired files in campus_media, campus_music, and image_cache, then enforce LRU size cap
        val lruCandidates = mutableListOf<File>()
        val mediaDir = File(cacheRoot, "campus_media")
        val musicDir = File(cacheRoot, "campus_music")
        val coilDir = File(cacheRoot, "image_cache")

        listOf(mediaDir, musicDir, coilDir).forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val age = now - file.lastModified()
                    if (file.name.endsWith(".tmp") && age > RECENT_PROTECTION_MS) {
                        val len = file.length()
                        if (file.delete()) freedBytes += len
                    } else if (age > maxAgeMs && age > RECENT_PROTECTION_MS) {
                        val len = file.length()
                        if (file.delete()) freedBytes += len
                    } else {
                        lruCandidates.add(file)
                    }
                }
            }
        }

        var currentSize = lruCandidates.sumOf { it.length() }
        if (currentSize > maxBytes) {
            val evictable = lruCandidates
                .filter { now - it.lastModified() > RECENT_PROTECTION_MS }
                .sortedBy { it.lastModified() }
            for (file in evictable) {
                if (currentSize <= targetBytes) break
                val len = file.length()
                if (file.delete()) {
                    currentSize -= len
                    freedBytes += len
                }
            }
        }

        freedBytes
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAllMediaCache(context: Context): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        val cacheRoot = context.cacheDir

        listOf("campus_media", "campus_music", "image_cache", "app_updates").forEach { folder ->
            val dir = File(cacheRoot, folder)
            if (dir.exists()) {
                dir.walkBottomUp().forEach { entry ->
                    if (entry != dir) {
                        val len = if (entry.isFile) entry.length() else 0L
                        if (entry.delete()) freedBytes += len
                    }
                }
            }
        }

        cacheRoot.listFiles()?.forEach { file ->
            if (file.isFile && isManagedTempFile(file.name)) {
                val len = file.length()
                if (file.delete()) freedBytes += len
            }
        }

        CampusMediaRepository.clearMemoryCache()
        runCatching {
            val loader = context.applicationContext.imageLoader
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }

        freedBytes
    }

    private fun fLen(file: File): Long = if (file.isFile) file.length() else 0L

    private fun isManagedTempFile(name: String): Boolean {
        return name.startsWith("voice_") ||
            name.startsWith("play_") ||
            name.startsWith("story_video_") ||
            name.startsWith("edit_") ||
            name.endsWith(".tmp")
    }
}
