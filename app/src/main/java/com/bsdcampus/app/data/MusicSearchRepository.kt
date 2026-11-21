package com.bsdcampus.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StoryMusicTrack(
    val trackId: Long = 0L,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val previewUrl: String,
    val durationMs: Long = 30_000L
)

object MusicSearchRepository {
    private const val ITUNES_SEARCH_BASE = "https://itunes.apple.com/search"

    val FEATURED_CHIPS = listOf(
        "Trending" to "bollywood hits",
        "Lo-Fi" to "lofi chill beats",
        "Pop" to "top pop hits",
        "Hip-Hop" to "hip hop hits",
        "Indie" to "indie acoustic",
        "Punjabi" to "punjabi hits"
    )

    suspend fun searchSongs(query: String): Result<List<StoryMusicTrack>> = withContext(Dispatchers.IO) {
        val clean = query.trim().ifBlank { "bollywood hits" }
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val endpoint = "$ITUNES_SEARCH_BASE?term=$encoded&media=music&entity=song&limit=25&country=IN"
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("Music search returned HTTP $code"))
            }

            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return@withContext Result.success(emptyList())
            val tracks = ArrayList<StoryMusicTrack>(results.length())
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val preview = obj.optString("previewUrl").trim()
                if (preview.isBlank()) continue
                val title = obj.optString("trackName").trim()
                val artist = obj.optString("artistName").trim()
                if (title.isBlank()) continue
                val art100 = obj.optString("artworkUrl100").trim()
                val highResArt = if (art100.contains("100x100")) {
                    art100.replace("100x100", "300x300")
                } else {
                    art100
                }
                val trackId = obj.optLong("trackId", i.toLong())
                val rawTimeMs = obj.optLong("trackTimeMillis", 30_000L)
                tracks.add(
                    StoryMusicTrack(
                        trackId = trackId,
                        title = title.take(120),
                        artist = artist.ifBlank { "Unknown Artist" }.take(120),
                        artworkUrl = highResArt.take(500),
                        previewUrl = preview.take(500),
                        durationMs = if (rawTimeMs > 0L) minOf(rawTimeMs, 30_000L) else 30_000L
                    )
                )
            }
            Result.success(tracks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
