package com.bsdcampus.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object MusicCacheManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadSemaphore = Semaphore(3)
    private val inFlight = ConcurrentHashMap<String, Deferred<File?>>()

    private fun musicDir(context: Context): File =
        File(context.applicationContext.cacheDir, "campus_music").apply { mkdirs() }

    private fun cacheKey(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun getCachedFileIfReady(context: Context, url: String?): File? {
        val clean = url?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) } ?: return null
        val file = File(musicDir(context), "${cacheKey(clean)}.m4a")
        if (file.exists() && file.length() > 1024L) {
            return file
        }
        return null
    }

    fun resolvePlayableUri(context: Context, url: String): Uri {
        val clean = url.trim()
        val cached = getCachedFileIfReady(context, clean)
        if (cached != null) {
            runCatching { cached.setLastModified(System.currentTimeMillis()) }
            return Uri.fromFile(cached)
        }
        prefetch(context, clean)
        return Uri.parse(clean)
    }

    fun prefetchUrls(context: Context, urls: Collection<String>) {
        if (urls.isEmpty()) return
        val appCtx = context.applicationContext
        urls.asSequence()
            .map { it.trim() }
            .filter { it.startsWith("http", ignoreCase = true) }
            .distinct()
            .take(10)
            .forEach { url ->
                prefetch(appCtx, url)
            }
    }

    fun prefetch(context: Context, url: String): Deferred<File?>? {
        val clean = url.trim()
        if (!clean.startsWith("http", ignoreCase = true)) return null
        getCachedFileIfReady(context, clean)?.let { existing ->
            return CompletableDeferred(existing)
        }
        inFlight[clean]?.let { return it }

        val appCtx = context.applicationContext
        val deferred = CompletableDeferred<File?>()
        val existing = inFlight.putIfAbsent(clean, deferred)
        if (existing != null) return existing

        scope.launch {
            try {
                val downloaded = downloadSemaphore.withPermit {
                    getCachedFileIfReady(appCtx, clean) ?: downloadToDisk(appCtx, clean)
                }
                deferred.complete(downloaded)
            } catch (_: Throwable) {
                deferred.complete(null)
            } finally {
                inFlight.remove(clean, deferred)
            }
        }
        return deferred
    }

    private fun downloadToDisk(context: Context, cleanUrl: String): File? {
        val dir = musicDir(context)
        val key = cacheKey(cleanUrl)
        val target = File(dir, "$key.m4a")
        if (target.exists() && target.length() > 1024L) return target
        val tmp = File(dir, "$key.tmp")

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) {
                return null
            }
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(16_384)
                    var total = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        total += read
                        if (total > 8L * 1024L * 1024L) {
                            runCatching { tmp.delete() }
                            return null
                        }
                        output.write(buf, 0, read)
                    }
                }
            }
            if (tmp.exists() && tmp.length() > 1024L) {
                if (target.exists()) target.delete()
                if (tmp.renameTo(target)) {
                    runCatching { target.setLastModified(System.currentTimeMillis()) }
                    return target
                }
            }
            runCatching { tmp.delete() }
            null
        } catch (_: Throwable) {
            runCatching { tmp.delete() }
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }
}
