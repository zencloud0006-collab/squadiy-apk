package com.bsdcampus.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

enum class StoryFilterPreset(val label: String) {
    ORIGINAL("Original"),
    WARM("Warm"),
    COOL("Cool"),
    VIVID("Vivid"),
    BW("B&W"),
    FADE("Fade"),
    DRAMA("Drama"),
    RETRO("Retro");

    fun androidColorMatrix(
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): ColorMatrix {
        val m = ColorMatrix()
        when (this) {
            ORIGINAL -> {}
            WARM -> m.setScale(1.12f, 1.03f, 0.88f, 1f)
            COOL -> m.setScale(0.90f, 1.02f, 1.14f, 1f)
            BW -> m.setSaturation(0f)
            VIVID -> m.setSaturation(1.45f)
            FADE -> {
                m.setSaturation(0.75f)
                m.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.9f, 0f, 0f, 0f, 18f,
                            0f, 0.9f, 0f, 0f, 18f,
                            0f, 0f, 0.92f, 0f, 24f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            DRAMA -> {
                m.setSaturation(0.82f)
                val c = 1.24f
                val t = 128f * (1f - c)
                m.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            RETRO -> {
                m.setSaturation(0.85f)
                m.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.08f, 0.08f, 0f, 0f, 10f,
                            0.04f, 1.02f, 0.02f, 0f, 6f,
                            0f, 0.06f, 0.84f, 0f, 12f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        }

        if (saturation != 1f && this != BW) {
            val sat = ColorMatrix().apply { setSaturation(saturation.coerceIn(0f, 2f)) }
            m.postConcat(sat)
        }

        val c = contrast.coerceIn(0.5f, 1.8f)
        val bOffset = brightness.coerceIn(-0.45f, 0.45f) * 255f
        val cShift = 128f * (1f - c) + bOffset
        if (c != 1f || bOffset != 0f) {
            m.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, cShift,
                        0f, c, 0f, 0f, cShift,
                        0f, 0f, c, 0f, cShift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        return m
    }

    fun composeColorMatrix(
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): androidx.compose.ui.graphics.ColorMatrix =
        androidx.compose.ui.graphics.ColorMatrix(androidColorMatrix(brightness, contrast, saturation).array)

    /** Subtle translucent tint overlay used on Video PlayerView (API 26+ safe). */
    fun videoTintColor(): Color = when (this) {
        ORIGINAL -> Color.Transparent
        WARM -> Color(0x28FF8A3D)
        COOL -> Color(0x283B82F6)
        VIVID -> Color(0x1AE1306C)
        BW -> Color(0x5518181B)
        FADE -> Color(0x26D4D4D8)
        DRAMA -> Color(0x36000000)
        RETRO -> Color(0x2EA16207)
    }
}

enum class StoryOverlayColor(
    val label: String,
    val bgColor: Color,
    val textColor: Color,
    val bgArgb: Int,
    val textArgb: Int
) {
    WHITE("White", Color(0xEEFFFFFF), Color(0xFF000000), 0xEEFFFFFF.toInt(), 0xFF000000.toInt()),
    BLACK("Black", Color(0xDF000000), Color(0xFFFFFFFF), 0xDF000000.toInt(), 0xFFFFFFFF.toInt()),
    BLUE("Blue", Color(0xEB0095F6), Color(0xFFFFFFFF), 0xEB0095F6.toInt(), 0xFFFFFFFF.toInt()),
    PINK("Pink", Color(0xEBE1306C), Color(0xFFFFFFFF), 0xEBE1306C.toInt(), 0xFFFFFFFF.toInt()),
    PURPLE("Purple", Color(0xEB833AB4), Color(0xFFFFFFFF), 0xEB833AB4.toInt(), 0xFFFFFFFF.toInt()),
    AMBER("Amber", Color(0xEBF59E0B), Color(0xFF111111), 0xEBF59E0B.toInt(), 0xFF111111.toInt()),
    GREEN("Green", Color(0xEB10B981), Color(0xFFFFFFFF), 0xEB10B981.toInt(), 0xFFFFFFFF.toInt()),
    RED("Red", Color(0xEBFF3040), Color(0xFFFFFFFF), 0xEBFF3040.toInt(), 0xFFFFFFFF.toInt())
}

enum class StoryTextStyle(val label: String) {
    PILL("Classic"),
    CLEAN("Shadow"),
    NEON("Neon"),
    MONO("Mono")
}

enum class StoryTextAlign(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

data class StoryOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isEmoji: Boolean = false,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val color: StoryOverlayColor = StoryOverlayColor.WHITE,
    val style: StoryTextStyle = StoryTextStyle.PILL,
    val align: StoryTextAlign = StoryTextAlign.CENTER
) {
    companion object {
        fun serializeList(items: List<StoryOverlayItem>): String {
            if (items.isEmpty()) return ""
            val arr = JSONArray()
            items.take(15).forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text.take(100))
                        put("emoji", item.isEmoji)
                        put("x", item.xFraction.toDouble())
                        put("y", item.yFraction.toDouble())
                        put("s", item.scale.toDouble())
                        put("r", item.rotationDeg.toDouble())
                        put("c", item.color.name)
                        put("st", item.style.name)
                        put("al", item.align.name)
                    }
                )
            }
            return arr.toString()
        }

        fun deserializeList(raw: String?): List<StoryOverlayItem> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val txt = obj.optString("text")
                        if (txt.isBlank()) continue
                        add(
                            StoryOverlayItem(
                                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                text = txt,
                                isEmoji = obj.optBoolean("emoji", false),
                                xFraction = obj.optDouble("x", 0.5).toFloat().coerceIn(0.05f, 0.95f),
                                yFraction = obj.optDouble("y", 0.5).toFloat().coerceIn(0.05f, 0.95f),
                                scale = obj.optDouble("s", 1.0).toFloat().coerceIn(0.45f, 3.0f),
                                rotationDeg = obj.optDouble("r", 0.0).toFloat(),
                                color = StoryOverlayColor.entries.firstOrNull { it.name == obj.optString("c") }
                                    ?: StoryOverlayColor.WHITE,
                                style = StoryTextStyle.entries.firstOrNull { it.name == obj.optString("st") }
                                    ?: StoryTextStyle.PILL,
                                align = StoryTextAlign.entries.firstOrNull { it.name == obj.optString("al") }
                                    ?: StoryTextAlign.CENTER
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class StoryBrushStroke(
    val points: List<Pair<Float, Float>>, // normalized 0f..1f coordinates relative to photo canvas
    val colorArgb: Int,
    val widthFraction: Float = 0.012f
)

data class StoryEditParams(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val filter: StoryFilterPreset = StoryFilterPreset.ORIGINAL,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val overlays: List<StoryOverlayItem> = emptyList(),
    val brushStrokes: List<StoryBrushStroke> = emptyList(),
    // Video edit parameters:
    val muteOriginalAudio: Boolean = false,
    val videoStartMs: Long = 0L,
    val videoEndMs: Long = 0L,
    val videoDurationMs: Long = 0L,
    val videoSpeed: Float = 1f,
    // Music parameters (applicable to both photo and video stories):
    val musicTrack: StoryMusicTrack? = null,
    val musicStartSec: Int = 0
)

object StoryMediaEditor {
    fun pendingStoriesDir(context: Context): File =
        File(context.applicationContext.cacheDir, "pending_stories").apply { mkdirs() }

    fun pendingUploadsDir(context: Context): File =
        File(context.applicationContext.cacheDir, "pending_uploads").apply { mkdirs() }

    /** Reads EXIF-aware decoded aspect ratio (width / height) cleanly on Dispatchers.IO. */
    suspend fun readImageAspectRatio(context: Context, uri: Uri, userRotationDegrees: Int): Float =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val exifRotation = resolver.openInputStream(uri)?.use { input ->
                    when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext 9f / 16f
                val totalRot = ((exifRotation + userRotationDegrees) % 360 + 360) % 360
                val swap = totalRot == 90 || totalRot == 270
                val w = if (swap) bounds.outHeight.toFloat() else bounds.outWidth.toFloat()
                val h = if (swap) bounds.outWidth.toFloat() else bounds.outHeight.toFloat()
                (w / h).coerceIn(0.35f, 2.5f)
            } catch (_: Exception) {
                9f / 16f
            }
        }

    /** Reads rotation-aware video natural aspect ratio (width / height) cleanly on Dispatchers.IO. */
    suspend fun readVideoAspectRatio(context: Context, uri: Uri): Float =
        withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val rawW = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
                val rawH = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
                val rot = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rawW <= 0f || rawH <= 0f) return@withContext 9f / 16f
                val swap = (rot % 180 + 180) % 180 == 90
                val w = if (swap) rawH else rawW
                val h = if (swap) rawW else rawH
                (w / h).coerceIn(0.45f, 2.2f)
            } catch (_: Exception) {
                9f / 16f
            } finally {
                runCatching { retriever.release() }
            }
        }

    /** Extracts a compact preview frame bitmap for local video thumbnails on Dispatchers.IO. */
    suspend fun extractVideoThumbnailBitmap(context: Context, uri: Uri, maxDim: Int = 420): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val frame = retriever.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                    ?: return@withContext null
                val srcMax = max(frame.width, frame.height)
                if (srcMax > maxDim && srcMax > 0) {
                    val scale = maxDim.toFloat() / srcMax.toFloat()
                    Bitmap.createScaledBitmap(
                        frame,
                        (frame.width * scale).roundToInt().coerceAtLeast(1),
                        (frame.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    ).also { if (it != frame) frame.recycle() }
                } else {
                    frame
                }
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun stageEditedImageToCache(
        context: Context,
        uri: Uri,
        uploadId: String,
        isAdmin: Boolean,
        edit: StoryEditParams,
        targetAspectRatio: Float? = null,
        outputDir: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val exifRotation = try {
                resolver.openInputStream(uri)?.use { input ->
                    when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
            } catch (_: Exception) {
                0
            }

            val maxDim = if (isAdmin) 1440 else 1080
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcMax = max(bounds.outWidth, bounds.outHeight)
            if (srcMax <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read selected image."))
            }

            var sampleSize = 1
            while (srcMax / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode selected image."))

            val totalRotation = ((exifRotation + edit.rotationDegrees) % 360 + 360) % 360
            val oriented = if (totalRotation != 0 || edit.flipHorizontal) {
                val matrix = Matrix().apply {
                    if (totalRotation != 0) postRotate(totalRotation.toFloat())
                    if (edit.flipHorizontal) postScale(-1f, 1f)
                }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val srcW = oriented.width
            val srcH = oriented.height
            val srcRect: Rect
            val croppedW: Int
            val croppedH: Int
            if (targetAspectRatio != null && targetAspectRatio > 0f && srcW > 0 && srcH > 0) {
                val currentRatio = srcW.toFloat() / srcH.toFloat()
                if (currentRatio > targetAspectRatio) {
                    croppedH = srcH
                    croppedW = (srcH * targetAspectRatio).roundToInt().coerceIn(1, srcW)
                    val left = ((srcW - croppedW) / 2).coerceAtLeast(0)
                    srcRect = Rect(left, 0, left + croppedW, srcH)
                } else {
                    croppedW = srcW
                    croppedH = (srcW / targetAspectRatio).roundToInt().coerceIn(1, srcH)
                    val top = ((srcH - croppedH) / 2).coerceAtLeast(0)
                    srcRect = Rect(0, top, srcW, top + croppedH)
                }
            } else {
                croppedW = srcW
                croppedH = srcH
                srcRect = Rect(0, 0, srcW, srcH)
            }

            val scale = if (max(croppedW, croppedH) > maxDim) {
                maxDim.toFloat() / max(croppedW, croppedH).toFloat()
            } else {
                1f
            }
            val targetW = (croppedW * scale).roundToInt().coerceAtLeast(1)
            val targetH = (croppedH * scale).roundToInt().coerceAtLeast(1)

            val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    edit.filter.androidColorMatrix(
                        brightness = edit.brightness,
                        contrast = edit.contrast,
                        saturation = edit.saturation
                    )
                )
            }
            canvas.drawBitmap(oriented, srcRect, RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()), paint)
            oriented.recycle()

            val minSide = minOf(targetW, targetH).toFloat()

            // Bake freehand brush strokes
            edit.brushStrokes.forEach { stroke ->
                if (stroke.points.isEmpty()) return@forEach
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.colorArgb
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = (minSide * stroke.widthFraction).coerceAtLeast(4f)
                }
                if (stroke.points.size == 1) {
                    val p = stroke.points.first()
                    strokePaint.style = Paint.Style.FILL
                    canvas.drawCircle(p.first * targetW, p.second * targetH, strokePaint.strokeWidth / 2f, strokePaint)
                } else {
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.first * targetW, first.second * targetH)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.first * targetW, pt.second * targetH)
                    }
                    canvas.drawPath(path, strokePaint)
                }
            }

            // Bake all draggable/scalable/rotatable text and emoji overlays
            edit.overlays.forEach { item ->
                val textVal = item.text.trim()
                if (textVal.isEmpty()) return@forEach
                val centerX = item.xFraction * targetW
                val centerY = item.yFraction * targetH

                canvas.save()
                canvas.translate(centerX, centerY)
                canvas.rotate(item.rotationDeg)
                canvas.scale(item.scale, item.scale)

                if (item.isEmoji) {
                    val emojiSize = (minSide * 0.13f).coerceIn(48f, 130f)
                    val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = emojiSize
                        textAlign = Paint.Align.CENTER
                    }
                    val fm = emojiPaint.fontMetrics
                    val baselineOffset = -(fm.ascent + fm.descent) / 2f
                    canvas.drawText(textVal, 0f, baselineOffset, emojiPaint)
                } else {
                    val textSizePx = (minSide * 0.056f).coerceIn(28f, 64f)
                    val fgColor = when (item.style) {
                        StoryTextStyle.PILL -> item.color.textArgb
                        StoryTextStyle.CLEAN, StoryTextStyle.NEON, StoryTextStyle.MONO -> {
                            if (item.color == StoryOverlayColor.BLACK) 0xFFFFFFFF.toInt() else item.color.bgArgb or 0xFF000000.toInt()
                        }
                    }
                    val tf = when (item.style) {
                        StoryTextStyle.MONO -> Typeface.MONOSPACE
                        else -> Typeface.DEFAULT_BOLD
                    }
                    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = fgColor
                        textSize = textSizePx
                        typeface = tf
                        isFakeBoldText = item.style != StoryTextStyle.MONO
                        if (item.style == StoryTextStyle.CLEAN) {
                            setShadowLayer(10f, 0f, 3f, 0xCC000000.toInt())
                        } else if (item.style == StoryTextStyle.NEON) {
                            setShadowLayer(20f, 0f, 0f, item.color.bgArgb or 0xFF000000.toInt())
                        }
                    }
                    val maxTextWidth = (targetW * 0.78f).roundToInt().coerceAtLeast(140)
                    val layoutAlign = when (item.align) {
                        StoryTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
                        StoryTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                        StoryTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    }
                    val layout = StaticLayout.Builder
                        .obtain(textVal, 0, textVal.length, textPaint, maxTextWidth)
                        .setAlignment(layoutAlign)
                        .build()

                    var maxLineW = 0f
                    for (i in 0 until layout.lineCount) {
                        maxLineW = max(maxLineW, layout.getLineWidth(i))
                    }
                    val padH = textSizePx * 0.52f
                    val padV = textSizePx * 0.32f
                    val boxW = maxLineW + padH * 2f
                    val boxH = layout.height + padV * 2f

                    if (item.style == StoryTextStyle.PILL) {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = item.color.bgArgb
                        }
                        val corner = textSizePx * 0.42f
                        canvas.drawRoundRect(
                            RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f),
                            corner,
                            corner,
                            bgPaint
                        )
                    } else if (item.style == StoryTextStyle.MONO) {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color(0xCC0B0B0E).toArgb()
                        }
                        canvas.drawRoundRect(
                            RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f),
                            14f,
                            14f,
                            bgPaint
                        )
                    }

                    canvas.translate(-maxTextWidth / 2f, -layout.height / 2f)
                    layout.draw(canvas)
                }
                canvas.restore()
            }

            val destDir = outputDir ?: pendingStoriesDir(context)
            destDir.mkdirs()
            val outFile = File(destDir, "$uploadId.jpg")
            FileOutputStream(outFile).use { fos ->
                output.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            output.recycle()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stageVideoToCache(
        context: Context,
        uri: Uri,
        uploadId: String,
        isAdmin: Boolean,
        outputDir: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val destDir = outputDir ?: pendingStoriesDir(context)
        destDir.mkdirs()
        val outFile = File(destDir, "$uploadId.mp4")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buf = ByteArray(65_536)
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                    }
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read selected video."))

            if (!outFile.exists() || outFile.length() == 0L) {
                runCatching { outFile.delete() }
                return@withContext Result.failure(IllegalArgumentException("Selected video file is empty."))
            }
            Result.success(outFile)
        } catch (e: Exception) {
            runCatching { outFile.delete() }
            Result.failure(e)
        }
    }
}
