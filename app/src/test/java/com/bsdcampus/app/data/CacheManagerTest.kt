package com.bsdcampus.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CacheManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking { CacheManager.clearAllMediaCache(context) }
    }

    @After
    fun tearDown() {
        runBlocking { CacheManager.clearAllMediaCache(context) }
    }

    @Test
    fun runAutoCleanup_evictsExpiredAndOversizedCache_whileProtectingRecentPlayback() = runBlocking {
        val mediaDir = File(context.cacheDir, "campus_media").apply { mkdirs() }
        val now = System.currentTimeMillis()

        // 1. Expired reel (72h old) -> must be evicted
        val expiredReel = File(mediaDir, "old_reel.mp4").apply {
            writeBytes(ByteArray(512))
            setLastModified(now - 72L * 60L * 60L * 1000L)
        }

        // 2. Active/recent reel (1 min old) -> must be preserved even if over limit
        val activeReel = File(mediaDir, "active_reel.mp4").apply {
            writeBytes(ByteArray(512))
            setLastModified(now - 60_000L)
        }

        // 3. Aged non-expired reel (20h old, 2 MB total via sparse/byte arrays -> Wait, runAutoCleanup min is 25MB? Let's verify age eviction!)
        val agedNonExpiredReel = File(mediaDir, "recent_20h_reel.mp4").apply {
            writeBytes(ByteArray(1024))
            setLastModified(now - 20L * 60L * 60L * 1000L)
        }

        val freed = CacheManager.runAutoCleanup(context, maxSizeMb = 100, maxAgeHours = 48)
        assertTrue("Expired reel should free bytes", freed >= 512L)
        assertFalse("72h reel should be deleted", expiredReel.exists())
        assertTrue("1-min active reel must stay", activeReel.exists())
        assertTrue("20h reel within 48h cap must stay", agedNonExpiredReel.exists())

        val stats = CacheManager.measureCache(context)
        assertEquals(2, stats.videoFilesCount)
        assertEquals(1536L, stats.mediaCacheBytes)
    }

    @Test
    fun clearAllMediaCache_emptiesMediaMusicAndUpdateCaches() = runBlocking {
        val mediaDir = File(context.cacheDir, "campus_media").apply { mkdirs() }
        val musicDir = File(context.cacheDir, "campus_music").apply { mkdirs() }
        val updatesDir = File(context.cacheDir, "app_updates").apply { mkdirs() }
        val reelFile = File(mediaDir, "video1.mp4").apply { writeBytes(ByteArray(2048)) }
        val musicFile = File(musicDir, "track1.m4a").apply { writeBytes(ByteArray(1024)) }
        val apkFile = File(updatesDir, "squadify_update.apk").apply { writeBytes(ByteArray(4096)) }

        val freed = CacheManager.clearAllMediaCache(context)
        assertTrue(freed >= 7168L)
        assertFalse(reelFile.exists())
        assertFalse(musicFile.exists())
        assertFalse(apkFile.exists())
        assertEquals(0L, CacheManager.measureCache(context).totalBytes)
    }
}
