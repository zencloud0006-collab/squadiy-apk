package com.bsdcampus.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object MediaUtils {
    const val VIDEO_URI_PREFIX = "bsdvideo://"
    const val IMAGE_ASSET_URI_PREFIX = "bsdmedia://"
    const val MAX_REGULAR_USER_VIDEO_BYTES = 25L * 1024L * 1024L
    const val CHUNK_RAW_BYTES = 450_000

    private const val MAX_DIMENSION = 900
    private const val MAX_BASE64_LENGTH = 260_000

    fun isVideoMediaUri(uri: String?): Boolean =
        uri?.startsWith(VIDEO_URI_PREFIX) == true

    fun isChunkedMediaUri(uri: String?): Boolean =
        uri?.startsWith(VIDEO_URI_PREFIX) == true || uri?.startsWith(IMAGE_ASSET_URI_PREFIX) == true

    fun extractAssetId(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        return when {
            uri.startsWith(VIDEO_URI_PREFIX) -> uri.removePrefix(VIDEO_URI_PREFIX).takeIf { it.isNotBlank() }
            uri.startsWith(IMAGE_ASSET_URI_PREFIX) -> uri.removePrefix(IMAGE_ASSET_URI_PREFIX).takeIf { it.isNotBlank() }
            else -> null
        }
    }

    fun isVideoContentUri(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri) ?: return false
        return type.startsWith("video/")
    }

    suspend fun compressUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION
    ): String? = withContext(Dispatchers.IO) {
        try {
            val decoded = decodeOrientedBitmap(context, uri, maxDimension) ?: return@withContext null

            var workingBitmap = scaleDownIfNeeded(decoded, maxDimension)
            var currentMaxDim = maxDimension
            var encoded = ""
            for (attempt in 0..3) {
                var quality = 82
                do {
                    val baos = ByteArrayOutputStream()
                    workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    encoded = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    quality -= 12
                } while (encoded.length > MAX_BASE64_LENGTH && quality >= 24)

                if (encoded.length <= 340_000) break
                currentMaxDim = (currentMaxDim * 0.72f).roundToInt().coerceAtLeast(160)
                val smaller = scaleDownIfNeeded(workingBitmap, currentMaxDim)
                if (smaller != workingBitmap) workingBitmap.recycle()
                workingBitmap = smaller
            }

            if (!workingBitmap.isRecycled) workingBitmap.recycle()
            if (encoded.length <= 340_000 && encoded.isNotBlank()) encoded else null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeOrientedBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            val decoded = runCatching {
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                    val w = info.size.width
                    val h = info.size.height
                    val srcMax = max(w, h)
                    if (srcMax > maxDimension && srcMax > 0) {
                        val scale = maxDimension.toFloat() / srcMax.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcMax = max(bounds.outWidth, bounds.outHeight)
        if (srcMax <= 0) return null
        var sampleSize = 1
        while (srcMax / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val srcMax = max(bitmap.width, bitmap.height)
        if (srcMax <= maxDimension || srcMax <= 0) return bitmap
        val scale = maxDimension.toFloat() / srcMax.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true
        ).also { if (it != bitmap) bitmap.recycle() }
    }

    suspend fun compressUriToJpegBytes(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1440,
        quality: Int = 85
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val decoded = decodeOrientedBitmap(context, uri, maxDimension) ?: return@withContext null
            val scaled = scaleDownIfNeeded(decoded, maxDimension)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (!scaled.isRecycled) scaled.recycle()
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractVideoThumbnailDataUrl(
        context: Context,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var fis: java.io.FileInputStream? = null
        try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                val f = java.io.File(uri.path!!)
                if (f.exists() && f.length() > 0L) {
                    fis = java.io.FileInputStream(f)
                    retriever.setDataSource(fis.fd)
                } else {
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return@withContext null
            val maxDim = 480
            val scale = if (max(frame.width, frame.height) > maxDim) {
                maxDim.toFloat() / max(frame.width, frame.height).toFloat()
            } else {
                1f
            }
            val thumb = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).roundToInt().coerceAtLeast(1),
                    (frame.height * scale).roundToInt().coerceAtLeast(1),
                    true
                ).also { if (it != frame) frame.recycle() }
            } else {
                frame
            }
            val baos = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, 72, baos)
            thumb.recycle()
            val encoded = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            if (encoded.length <= 340_000) encoded else null
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { fis?.close() }
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun readUriBytesWithLimit(
        context: Context,
        uri: Uri,
        maxBytes: Long?
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val baos = ByteArrayOutputStream()
                val buf = ByteArray(16_384)
                var total = 0L
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    total += read
                    if (maxBytes != null && total > maxBytes) {
                        return@withContext null
                    }
                    baos.write(buf, 0, read)
                }
                baos.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUriBytes(dataUri: String?): ByteArray? {
        if (dataUri.isNullOrBlank() || !dataUri.startsWith("data:")) return null
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0) return null
        return try {
            Base64.decode(dataUri.substring(commaIndex + 1), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun encodeAudioBytesToDataUrl(bytes: ByteArray): String? {
        val dataUrl = "data:audio/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        return if (dataUrl.length <= 340_000) dataUrl else null
    }
}
