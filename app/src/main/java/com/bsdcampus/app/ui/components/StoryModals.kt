package com.bsdcampus.app.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.MusicCacheManager
import com.bsdcampus.app.data.StoryBrushStroke
import com.bsdcampus.app.data.StoryEditParams
import com.bsdcampus.app.data.StoryFilterPreset
import com.bsdcampus.app.data.StoryMediaEditor
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.data.StoryOverlayColor
import com.bsdcampus.app.data.StoryOverlayItem
import com.bsdcampus.app.data.StoryTextAlign
import com.bsdcampus.app.data.StoryTextStyle
import com.bsdcampus.app.model.CampusStory
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.StoryGroup
import com.bsdcampus.app.notifications.StoryUploadWorker
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import com.bsdcampus.app.ui.theme.InstaOrange
import com.bsdcampus.app.ui.theme.InstaPink
import com.bsdcampus.app.ui.theme.InstaPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class StoryEditPanel {
    NONE,
    FILTERS,
    ADJUST,
    TRANSFORM,
    TEXT,
    STICKERS,
    DRAW,
    TRIM,
    SPEED
}

private val STORY_EMOJI_STICKERS = listOf(
    "🔥", "❤️", "😂", "😍", "🎉", "✨", "🎓", "🏫", "📚", "🏆", "🎶", "😎", "🤩", "🙌", "💯", "🥳"
)

private val VIDEO_SPEED_OPTIONS = listOf(0.5f, 1.0f, 1.5f, 2.0f)

@Composable
fun StoryCreatorDialog(
    currentUser: CampusUser,
    isAdmin: Boolean,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var imageAspectRatio by remember { mutableFloatStateOf(9f / 16f) }
    var caption by remember { mutableStateOf("") }
    var isPreparing by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(StoryEditPanel.NONE) }
    var editParams by remember { mutableStateOf(StoryEditParams()) }
    var showMusicSheet by remember { mutableStateOf(false) }

    // Draggable overlay + text editing state
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }
    var draggingOverlayId by remember { mutableStateOf<String?>(null) }
    var isDragOverTrash by remember { mutableStateOf(false) }

    var editingTextId by remember { mutableStateOf<String?>(null) }
    var draftText by remember { mutableStateOf("") }
    var draftColor by remember { mutableStateOf(StoryOverlayColor.WHITE) }
    var draftStyle by remember { mutableStateOf(StoryTextStyle.PILL) }
    var draftAlign by remember { mutableStateOf(StoryTextAlign.CENTER) }

    // Freehand drawing brush state
    var brushColor by remember { mutableStateOf(StoryOverlayColor.PINK) }
    var brushWidthFraction by remember { mutableFloatStateOf(0.013f) }
    var activeBrushPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            isVideo = MediaUtils.isVideoContentUri(context, uri)
            editParams = StoryEditParams()
            activePanel = StoryEditPanel.NONE
            selectedOverlayId = null
            editingTextId = null
            draftText = ""
            activeBrushPoints = emptyList()
        }
    }

    LaunchedEffect(pickedUri, isVideo, editParams.rotationDegrees) {
        val u = pickedUri
        if (u != null && !isVideo) {
            imageAspectRatio = StoryMediaEditor.readImageAspectRatio(context, u, editParams.rotationDegrees)
        }
    }

    // Live background music preview inside creator when a song is picked
    val creatorMusicTrack = editParams.musicTrack
    val creatorMusicPlayer = remember(creatorMusicTrack?.previewUrl) {
        creatorMusicTrack?.let { track ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(track.previewUrl))
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
            }
        }
    }
    DisposableEffect(creatorMusicPlayer) {
        onDispose { creatorMusicPlayer?.release() }
    }
    LaunchedEffect(creatorMusicPlayer, editParams.musicStartSec, showMusicSheet, isPreparing) {
        val player = creatorMusicPlayer ?: return@LaunchedEffect
        if (showMusicSheet || isPreparing) {
            player.pause()
        } else {
            player.seekTo(editParams.musicStartSec * 1000L)
            player.playWhenReady = true
            player.play()
        }
    }

    fun openTextComposer(existing: StoryOverlayItem? = null) {
        if (existing != null && !existing.isEmoji) {
            editingTextId = existing.id
            draftText = existing.text
            draftColor = existing.color
            draftStyle = existing.style
            draftAlign = existing.align
        } else {
            editingTextId = null
            draftText = ""
            draftColor = StoryOverlayColor.WHITE
            draftStyle = StoryTextStyle.PILL
            draftAlign = StoryTextAlign.CENTER
        }
        activePanel = StoryEditPanel.TEXT
    }

    fun commitTextOverlay() {
        val clean = draftText.trim()
        if (clean.isEmpty()) {
            editingTextId?.let { targetId ->
                editParams = editParams.copy(overlays = editParams.overlays.filterNot { it.id == targetId })
            }
            activePanel = StoryEditPanel.NONE
            editingTextId = null
            return
        }
        val currentTargetId = editingTextId
        val nextOverlays = if (currentTargetId != null && editParams.overlays.any { it.id == currentTargetId }) {
            editParams.overlays.map { item ->
                if (item.id == currentTargetId) {
                    item.copy(
                        text = clean,
                        color = draftColor,
                        style = draftStyle,
                        align = draftAlign
                    )
                } else item
            }
        } else {
            val offsetIdx = editParams.overlays.size % 4
            val newItem = StoryOverlayItem(
                text = clean,
                isEmoji = false,
                xFraction = 0.5f,
                yFraction = (0.34f + offsetIdx * 0.10f).coerceIn(0.2f, 0.72f),
                color = draftColor,
                style = draftStyle,
                align = draftAlign
            )
            selectedOverlayId = newItem.id
            editParams.overlays + newItem
        }
        editParams = editParams.copy(overlays = nextOverlays)
        draftText = ""
        editingTextId = null
        activePanel = StoryEditPanel.NONE
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Media Preview + Interactive Overlay Canvas
            val bottomReserved = when {
                pickedUri == null -> 88.dp
                activePanel != StoryEditPanel.NONE -> 230.dp
                else -> 148.dp
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = bottomReserved),
                contentAlignment = Alignment.Center
            ) {
                val activeUri = pickedUri
                if (activeUri != null) {
                    if (isVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            LocalStoryVideoPreview(
                                uri = activeUri,
                                muteAudio = editParams.muteOriginalAudio,
                                startMs = editParams.videoStartMs,
                                endMs = editParams.videoEndMs,
                                playbackSpeed = editParams.videoSpeed,
                                paused = showMusicSheet || isPreparing,
                                onDurationDetected = { durMs ->
                                    if (durMs > 0L && editParams.videoDurationMs != durMs) {
                                        val initialEnd = if (editParams.videoEndMs > 0L) {
                                            editParams.videoEndMs.coerceAtMost(durMs)
                                        } else durMs
                                        editParams = editParams.copy(
                                            videoDurationMs = durMs,
                                            videoEndMs = initialEnd
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            val tint = editParams.filter.videoTintColor()
                            if (tint != Color.Transparent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(tint)
                                )
                            }

                            StoryOverlayCanvas(
                                overlays = editParams.overlays,
                                selectedId = selectedOverlayId,
                                interactive = activePanel != StoryEditPanel.DRAW,
                                onSelect = { item ->
                                    selectedOverlayId = item?.id
                                    if (item != null && !item.isEmoji) {
                                        openTextComposer(item)
                                    }
                                },
                                onUpdateItem = { updated ->
                                    editParams = editParams.copy(
                                        overlays = editParams.overlays.map { if (it.id == updated.id) updated else it }
                                    )
                                },
                                onDeleteItem = { id ->
                                    editParams = editParams.copy(
                                        overlays = editParams.overlays.filterNot { it.id == id }
                                    )
                                    if (selectedOverlayId == id) selectedOverlayId = null
                                },
                                onDragStateChange = { dragId, overTrash ->
                                    draggingOverlayId = dragId
                                    isDragOverTrash = overTrash
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Aspect-ratio locked container so normalized (0f..1f) overlay & brush coords match exported bitmap 1:1
                        Box(
                            modifier = Modifier
                                .aspectRatio(imageAspectRatio)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = activeUri,
                                contentDescription = "Story photo preview",
                                contentScale = ContentScale.FillBounds,
                                colorFilter = ColorFilter.colorMatrix(
                                    editParams.filter.composeColorMatrix(
                                        brightness = editParams.brightness,
                                        contrast = editParams.contrast,
                                        saturation = editParams.saturation
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = editParams.rotationDegrees.toFloat()
                                        scaleX = if (editParams.flipHorizontal) -1f else 1f
                                    }
                            )

                            // Freehand brush strokes layer
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (activePanel == StoryEditPanel.DRAW) {
                                            Modifier.pointerInput(brushColor, brushWidthFraction) {
                                                detectDragGestures(
                                                    onDragStart = { start ->
                                                        val nx = (start.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val ny = (start.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                        activeBrushPoints = listOf(nx to ny)
                                                    },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        val nx = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val ny = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                        activeBrushPoints = activeBrushPoints + (nx to ny)
                                                    },
                                                    onDragEnd = {
                                                        if (activeBrushPoints.isNotEmpty()) {
                                                            editParams = editParams.copy(
                                                                brushStrokes = editParams.brushStrokes + StoryBrushStroke(
                                                                    points = activeBrushPoints,
                                                                    colorArgb = brushColor.bgArgb or 0xFF000000.toInt(),
                                                                    widthFraction = brushWidthFraction
                                                                )
                                                            )
                                                            activeBrushPoints = emptyList()
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        activeBrushPoints = emptyList()
                                                    }
                                                )
                                            }
                                        } else Modifier
                                    )
                            ) {
                                val minSide = minOf(size.width, size.height)
                                editParams.brushStrokes.forEach { stroke ->
                                    if (stroke.points.size == 1) {
                                        val pt = stroke.points.first()
                                        drawCircle(
                                            color = Color(stroke.colorArgb),
                                            radius = (minSide * stroke.widthFraction).coerceAtLeast(4f) / 2f,
                                            center = Offset(pt.first * size.width, pt.second * size.height)
                                        )
                                    } else if (stroke.points.size > 1) {
                                        val p = Path().apply {
                                            val f = stroke.points.first()
                                            moveTo(f.first * size.width, f.second * size.height)
                                            for (i in 1 until stroke.points.size) {
                                                lineTo(stroke.points[i].first * size.width, stroke.points[i].second * size.height)
                                            }
                                        }
                                        drawPath(
                                            path = p,
                                            color = Color(stroke.colorArgb),
                                            style = Stroke(
                                                width = (minSide * stroke.widthFraction).coerceAtLeast(4f),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                                if (activeBrushPoints.size > 1) {
                                    val p = Path().apply {
                                        val f = activeBrushPoints.first()
                                        moveTo(f.first * size.width, f.second * size.height)
                                        for (i in 1 until activeBrushPoints.size) {
                                            lineTo(activeBrushPoints[i].first * size.width, activeBrushPoints[i].second * size.height)
                                        }
                                    }
                                    drawPath(
                                        path = p,
                                        color = Color(brushColor.bgArgb or 0xFF000000.toInt()),
                                        style = Stroke(
                                            width = (minSide * brushWidthFraction).coerceAtLeast(4f),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }

                            // Interactive draggable, scalable, rotatable text + emoji stickers
                            StoryOverlayCanvas(
                                overlays = editParams.overlays,
                                selectedId = selectedOverlayId,
                                interactive = activePanel != StoryEditPanel.DRAW,
                                onSelect = { item ->
                                    selectedOverlayId = item?.id
                                    if (item != null && !item.isEmoji) {
                                        openTextComposer(item)
                                    }
                                },
                                onUpdateItem = { updated ->
                                    editParams = editParams.copy(
                                        overlays = editParams.overlays.map { if (it.id == updated.id) updated else it }
                                    )
                                },
                                onDeleteItem = { id ->
                                    editParams = editParams.copy(
                                        overlays = editParams.overlays.filterNot { it.id == id }
                                    )
                                    if (selectedOverlayId == id) selectedOverlayId = null
                                },
                                onDragStateChange = { dragId, overTrash ->
                                    draggingOverlayId = dragId
                                    isDragOverTrash = overTrash
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Music Sticker Pill floating near top-center when a song is chosen
                    editParams.musicTrack?.let { track ->
                        StoryMusicStickerPill(
                            title = track.title,
                            artist = track.artist,
                            artworkUrl = track.artworkUrl,
                            isPlaying = !showMusicSheet && !isPreparing,
                            onClick = { showMusicSheet = true },
                            onRemove = {
                                editParams = editParams.copy(musicTrack = null, musicStartSec = 0)
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                        )
                    }

                    // Drag-to-Trash Drop Zone
                    AnimatedVisibility(
                        visible = draggingOverlayId != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                    ) {
                        val trashScale by animateFloatAsState(
                            targetValue = if (isDragOverTrash) 1.24f else 1f,
                            label = "trashScale"
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isDragOverTrash) InstaLikeRed else Color.Black.copy(alpha = 0.72f),
                            border = BorderStroke(1.2.dp, if (isDragOverTrash) Color.White else Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .scale(trashScale)
                                .size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Drop here to remove item",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // Subtle drag hint when overlays exist and none is currently being dragged
                    if (editParams.overlays.isNotEmpty() && draggingOverlayId == null && activePanel == StoryEditPanel.NONE) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.48f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Drag, pinch or rotate text & stickers • Tap text to edit",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF121212))
                            .border(0.75.dp, Color(0xFF262626), RoundedCornerShape(20.dp))
                            .clickable(enabled = !isPreparing) {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .padding(horizontal = 28.dp, vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Pick story photo or video",
                            tint = InstaBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Add Photo or Video Story",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add music, draggable text & stickers, draw, trim & speed-adjust video — uploads in background!",
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close story creator", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserAvatar(
                        src = currentUser.avatar,
                        name = currentUser.name,
                        role = currentUser.role,
                        isVerified = currentUser.isVerified,
                        isBlackSociety = currentUser.isBlackSociety,
                        blackSocietyNumber = currentUser.blackSocietyNumber,
                        isClassRepresentative = currentUser.isClassRepresentative,
                        size = AvatarSize.SM
                    )
                    Text(
                        text = "Your Story",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    enabled = !isPreparing,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (pickedUri == null) "Choose" else "Change", color = Color.White, fontSize = 12.sp)
                }
            }

            // Bottom Editing Toolbar + Caption + Background Share CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEB08080A))
                    .align(Alignment.BottomCenter)
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pickedUri != null) {
                    when (activePanel) {
                        StoryEditPanel.FILTERS -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StoryFilterPreset.entries.forEach { preset ->
                                    val selected = editParams.filter == preset
                                    FilterChip(
                                        selected = selected,
                                        onClick = { editParams = editParams.copy(filter = preset) },
                                        label = { Text(preset.label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = InstaBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1A1A20),
                                            labelColor = Color.White.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                            }
                        }

                        StoryEditPanel.ADJUST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                AdjustSliderRow(
                                    label = "Light",
                                    value = editParams.brightness,
                                    range = -0.35f..0.35f,
                                    onChange = { editParams = editParams.copy(brightness = it) }
                                )
                                AdjustSliderRow(
                                    label = "Contrast",
                                    value = editParams.contrast,
                                    range = 0.6f..1.6f,
                                    onChange = { editParams = editParams.copy(contrast = it) }
                                )
                                AdjustSliderRow(
                                    label = "Color",
                                    value = editParams.saturation,
                                    range = 0f..1.8f,
                                    onChange = { editParams = editParams.copy(saturation = it) }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        editParams = editParams.copy(brightness = 0f, contrast = 1f, saturation = 1f)
                                    }) {
                                        Text("Reset All", color = InstaBlue, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { activePanel = StoryEditPanel.NONE }) {
                                        Text("Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        StoryEditPanel.TRANSFORM -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StoryToolButton(
                                    icon = Icons.AutoMirrored.Filled.RotateRight,
                                    label = "Rotate 90°",
                                    active = editParams.rotationDegrees != 0,
                                    onClick = {
                                        editParams = editParams.copy(rotationDegrees = (editParams.rotationDegrees + 90) % 360)
                                    }
                                )
                                StoryToolButton(
                                    icon = Icons.Filled.Flip,
                                    label = "Flip",
                                    active = editParams.flipHorizontal,
                                    onClick = {
                                        editParams = editParams.copy(flipHorizontal = !editParams.flipHorizontal)
                                    }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { activePanel = StoryEditPanel.NONE }) {
                                    Text("Done", color = InstaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        StoryEditPanel.TEXT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draftText,
                                    onValueChange = { draftText = it.take(100) },
                                    placeholder = {
                                        Text("Type text (drag anywhere after adding)...", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = InstaBlue,
                                        unfocusedBorderColor = Color(0xFF33333D)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("story_text_overlay_input")
                                )
                                // Style + Alignment row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StoryTextStyle.entries.forEach { st ->
                                        val selected = draftStyle == st
                                        FilterChip(
                                            selected = selected,
                                            onClick = { draftStyle = st },
                                            label = { Text(st.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InstaBlue,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1A1A22),
                                                labelColor = Color.White.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                    StoryTextAlign.entries.forEach { al ->
                                        val selected = draftAlign == al
                                        FilterChip(
                                            selected = selected,
                                            onClick = { draftAlign = al },
                                            label = { Text(al.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InstaPink.copy(alpha = 0.35f),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1A1A22),
                                                labelColor = Color.White.copy(alpha = 0.75f)
                                            )
                                        )
                                    }
                                }
                                // Color swatches + Cancel/Add CTA
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StoryOverlayColor.entries.forEach { col ->
                                        val selected = draftColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(col.bgColor)
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) InstaBlue else Color.White.copy(alpha = 0.45f),
                                                    shape = CircleShape
                                                )
                                                .clickable { draftColor = col }
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        draftText = ""
                                        editingTextId = null
                                        activePanel = StoryEditPanel.NONE
                                    }) {
                                        Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { commitTextOverlay() },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("story_add_text_confirm")
                                    ) {
                                        Text(
                                            text = if (editingTextId != null) "Save" else "Add",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        StoryEditPanel.STICKERS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    STORY_EMOJI_STICKERS.forEach { emoji ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF1A1A22),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clickable {
                                                    val idx = editParams.overlays.size % 5
                                                    val sticker = StoryOverlayItem(
                                                        text = emoji,
                                                        isEmoji = true,
                                                        xFraction = (0.35f + idx * 0.08f).coerceIn(0.25f, 0.75f),
                                                        yFraction = (0.38f + idx * 0.06f).coerceIn(0.25f, 0.72f),
                                                        scale = 1.15f
                                                    )
                                                    editParams = editParams.copy(overlays = editParams.overlays + sticker)
                                                    selectedOverlayId = sticker.id
                                                    activePanel = StoryEditPanel.NONE
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(emoji, fontSize = 22.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        StoryEditPanel.DRAW -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StoryOverlayColor.entries.forEach { col ->
                                        val selected = brushColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(col.bgColor)
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) InstaBlue else Color.White.copy(alpha = 0.45f),
                                                    shape = CircleShape
                                                )
                                                .clickable { brushColor = col }
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            if (editParams.brushStrokes.isNotEmpty()) {
                                                editParams = editParams.copy(brushStrokes = editParams.brushStrokes.dropLast(1))
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke", tint = Color.White)
                                    }
                                    TextButton(onClick = { activePanel = StoryEditPanel.NONE }) {
                                        Text("Done", color = InstaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Brush size", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                    listOf(0.008f to "Thin", 0.015f to "Med", 0.028f to "Bold").forEach { (w, lbl) ->
                                        FilterChip(
                                            selected = brushWidthFraction == w,
                                            onClick = { brushWidthFraction = w },
                                            label = { Text(lbl, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InstaBlue,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1A1A22),
                                                labelColor = Color.White.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                    if (editParams.brushStrokes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = { editParams = editParams.copy(brushStrokes = emptyList()) }) {
                                            Text("Clear", color = InstaLikeRed, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        StoryEditPanel.TRIM -> {
                            val totalDur = editParams.videoDurationMs.coerceAtLeast(3_000L)
                            val curStart = editParams.videoStartMs.coerceIn(0L, totalDur - 1_000L)
                            val curEnd = (if (editParams.videoEndMs > curStart) editParams.videoEndMs else totalDur)
                                .coerceIn(curStart + 1_000L, totalDur)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Trim: ${curStart / 1000}s – ${curEnd / 1000}s (${((curEnd - curStart) / 1000f).let { "%.1fs".format(it) }})",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = {
                                        editParams = editParams.copy(videoStartMs = 0L, videoEndMs = totalDur)
                                    }) {
                                        Text("Reset", color = InstaBlue, fontSize = 11.sp)
                                    }
                                }
                                RangeSlider(
                                    value = curStart.toFloat()..curEnd.toFloat(),
                                    onValueChange = { range ->
                                        val s = range.start.toLong().coerceAtLeast(0L)
                                        val e = range.endInclusive.toLong().coerceAtLeast(s + 1_000L).coerceAtMost(totalDur)
                                        editParams = editParams.copy(videoStartMs = s, videoEndMs = e)
                                    },
                                    valueRange = 0f..totalDur.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = InstaBlue,
                                        activeTrackColor = InstaBlue
                                    )
                                )
                            }
                        }

                        StoryEditPanel.SPEED -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Speed", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                VIDEO_SPEED_OPTIONS.forEach { spd ->
                                    val selected = editParams.videoSpeed == spd
                                    FilterChip(
                                        selected = selected,
                                        onClick = { editParams = editParams.copy(videoSpeed = spd) },
                                        label = { Text("${spd}x", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = InstaBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1A1A22),
                                            labelColor = Color.White.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                            }
                        }

                        StoryEditPanel.NONE -> {}
                    }

                    // Horizontal Edit Tool Strip (adapts cleanly between Photo tools & Video tools)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StoryToolButton(
                            icon = Icons.Filled.MusicNote,
                            label = if (editParams.musicTrack != null) "Music ✓" else "Music",
                            active = editParams.musicTrack != null,
                            onClick = { showMusicSheet = true },
                            testTag = "story_tool_music"
                        )
                        StoryToolButton(
                            icon = Icons.Filled.TextFields,
                            label = "Text",
                            active = activePanel == StoryEditPanel.TEXT || editParams.overlays.any { !it.isEmoji },
                            onClick = {
                                if (activePanel == StoryEditPanel.TEXT) {
                                    activePanel = StoryEditPanel.NONE
                                } else {
                                    openTextComposer(null)
                                }
                            },
                            testTag = "story_tool_text"
                        )
                        StoryToolButton(
                            icon = Icons.Filled.EmojiEmotions,
                            label = "Stickers",
                            active = activePanel == StoryEditPanel.STICKERS || editParams.overlays.any { it.isEmoji },
                            onClick = {
                                activePanel = if (activePanel == StoryEditPanel.STICKERS) StoryEditPanel.NONE else StoryEditPanel.STICKERS
                            },
                            testTag = "story_tool_stickers"
                        )
                        StoryToolButton(
                            icon = Icons.Filled.AutoFixHigh,
                            label = if (editParams.filter == StoryFilterPreset.ORIGINAL) "Filter" else editParams.filter.label,
                            active = activePanel == StoryEditPanel.FILTERS || editParams.filter != StoryFilterPreset.ORIGINAL,
                            onClick = {
                                activePanel = if (activePanel == StoryEditPanel.FILTERS) StoryEditPanel.NONE else StoryEditPanel.FILTERS
                            }
                        )

                        if (isVideo) {
                            StoryToolButton(
                                icon = if (editParams.muteOriginalAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                label = if (editParams.muteOriginalAudio) "Sound Off" else "Sound On",
                                active = editParams.muteOriginalAudio,
                                onClick = {
                                    editParams = editParams.copy(muteOriginalAudio = !editParams.muteOriginalAudio)
                                },
                                testTag = "story_tool_video_mute"
                            )
                            StoryToolButton(
                                icon = Icons.Filled.ContentCut,
                                label = "Trim",
                                active = activePanel == StoryEditPanel.TRIM || editParams.videoStartMs > 0L ||
                                    (editParams.videoDurationMs > 0L && editParams.videoEndMs in 1L until editParams.videoDurationMs),
                                onClick = {
                                    activePanel = if (activePanel == StoryEditPanel.TRIM) StoryEditPanel.NONE else StoryEditPanel.TRIM
                                },
                                testTag = "story_tool_video_trim"
                            )
                            StoryToolButton(
                                icon = Icons.Filled.Speed,
                                label = "${editParams.videoSpeed}x",
                                active = activePanel == StoryEditPanel.SPEED || editParams.videoSpeed != 1f,
                                onClick = {
                                    activePanel = if (activePanel == StoryEditPanel.SPEED) StoryEditPanel.NONE else StoryEditPanel.SPEED
                                },
                                testTag = "story_tool_video_speed"
                            )
                        } else {
                            StoryToolButton(
                                icon = Icons.Filled.Brush,
                                label = "Draw",
                                active = activePanel == StoryEditPanel.DRAW || editParams.brushStrokes.isNotEmpty(),
                                onClick = {
                                    activePanel = if (activePanel == StoryEditPanel.DRAW) StoryEditPanel.NONE else StoryEditPanel.DRAW
                                },
                                testTag = "story_tool_draw"
                            )
                            StoryToolButton(
                                icon = Icons.Filled.Tune,
                                label = "Adjust",
                                active = activePanel == StoryEditPanel.ADJUST ||
                                    editParams.brightness != 0f || editParams.contrast != 1f || editParams.saturation != 1f,
                                onClick = {
                                    activePanel = if (activePanel == StoryEditPanel.ADJUST) StoryEditPanel.NONE else StoryEditPanel.ADJUST
                                }
                            )
                            StoryToolButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                label = "Rotate/Flip",
                                active = activePanel == StoryEditPanel.TRANSFORM || editParams.rotationDegrees != 0 || editParams.flipHorizontal,
                                onClick = {
                                    activePanel = if (activePanel == StoryEditPanel.TRANSFORM) StoryEditPanel.NONE else StoryEditPanel.TRANSFORM
                                }
                            )
                        }
                    }
                }

                // Caption + Share in Background CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(300) },
                        placeholder = { Text("Add a caption...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.55f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                            cursorColor = InstaBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val activeUri = pickedUri ?: return@Button
                            isPreparing = true
                            val uploadId = UUID.randomUUID().toString()
                            scope.launch {
                                val stagedResult = if (isVideo) {
                                    StoryMediaEditor.stageVideoToCache(context, activeUri, uploadId, isAdmin)
                                } else {
                                    StoryMediaEditor.stageEditedImageToCache(context, activeUri, uploadId, isAdmin, editParams)
                                }
                                isPreparing = false
                                stagedResult.onSuccess { stagedFile ->
                                    val videoOverlays = if (isVideo && editParams.overlays.isNotEmpty()) {
                                        StoryOverlayItem.serializeList(editParams.overlays)
                                    } else null
                                    StoryUploadWorker.enqueue(
                                        context = context,
                                        uploadId = uploadId,
                                        stagedFile = stagedFile,
                                        isVideo = isVideo,
                                        caption = caption.trim(),
                                        isAdmin = isAdmin,
                                        musicTrack = editParams.musicTrack,
                                        musicStartSec = editParams.musicStartSec,
                                        muteOriginalAudio = isVideo && editParams.muteOriginalAudio,
                                        videoStartMs = if (isVideo) editParams.videoStartMs else 0L,
                                        videoEndMs = if (isVideo) editParams.videoEndMs else 0L,
                                        videoSpeed = if (isVideo) editParams.videoSpeed.toDouble() else 1.0,
                                        videoFilter = if (isVideo && editParams.filter != StoryFilterPreset.ORIGINAL) editParams.filter.name else null,
                                        videoOverlaysJson = videoOverlays
                                    )
                                    onShowToast("Uploading story in background...")
                                    onDismiss()
                                }.onFailure { err ->
                                    onShowToast(err.localizedMessage ?: "Failed to prepare story")
                                }
                            }
                        },
                        enabled = pickedUri != null && !isPreparing,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_story_button")
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isPreparing) "Saving..." else "Share Story", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showMusicSheet) {
        StoryMusicPickerSheet(
            currentTrack = editParams.musicTrack,
            currentStartSec = editParams.musicStartSec,
            allowMuteVideoAudio = isVideo,
            muteOriginalAudio = editParams.muteOriginalAudio,
            onApply = { track, startSec, muteOriginal ->
                editParams = editParams.copy(
                    musicTrack = track,
                    musicStartSec = startSec,
                    muteOriginalAudio = if (isVideo) muteOriginal else false
                )
            },
            onDismiss = { showMusicSheet = false }
        )
    }
}

@Composable
private fun StoryOverlayCanvas(
    overlays: List<StoryOverlayItem>,
    selectedId: String?,
    interactive: Boolean,
    onSelect: (StoryOverlayItem?) -> Unit,
    onUpdateItem: (StoryOverlayItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDragStateChange: (draggingId: String?, overTrash: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier.then(
            if (interactive) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { onSelect(null) })
                }
            } else Modifier
        )
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val canvasHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

        overlays.forEach { item ->
            var itemSize by remember(item.id) { mutableStateOf(IntSize.Zero) }
            val latestItem by rememberUpdatedState(item)
            val isSelected = interactive && selectedId == item.id

            val baseModifier = Modifier
                .onSizeChanged { itemSize = it }
                .offset {
                    IntOffset(
                        x = (item.xFraction * canvasWidthPx - itemSize.width / 2f).roundToInt(),
                        y = (item.yFraction * canvasHeightPx - itemSize.height / 2f).roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleX = item.scale
                    scaleY = item.scale
                    rotationZ = item.rotationDeg
                }
                .then(
                    if (interactive) {
                        Modifier
                            .pointerInput(item.id, canvasWidthPx, canvasHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val cur = latestItem
                                    val rad = Math.toRadians(cur.rotationDeg.toDouble())
                                    val cosA = cos(rad).toFloat()
                                    val sinA = sin(rad).toFloat()
                                    // Convert local rotated/scaled gesture delta back to parent canvas coordinates
                                    val dxParent = (pan.x * cosA - pan.y * sinA) * cur.scale
                                    val dyParent = (pan.x * sinA + pan.y * cosA) * cur.scale

                                    val nextX = (cur.xFraction + dxParent / canvasWidthPx).coerceIn(0.05f, 0.95f)
                                    val nextY = (cur.yFraction + dyParent / canvasHeightPx).coerceIn(0.05f, 0.94f)
                                    val nextScale = (cur.scale * zoom).coerceIn(0.45f, 3.0f)
                                    val nextRot = cur.rotationDeg + rotation
                                    val overTrash = nextY > 0.83f && nextX in 0.30f..0.70f

                                    onDragStateChange(cur.id, overTrash)
                                    onUpdateItem(
                                        cur.copy(
                                            xFraction = nextX,
                                            yFraction = nextY,
                                            scale = nextScale,
                                            rotationDeg = nextRot
                                        )
                                    )
                                }
                            }
                            .pointerInput(item.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Final)
                                        val anyPressed = event.changes.any { it.pressed }
                                        if (!anyPressed) {
                                            val cur = latestItem
                                            if (cur.yFraction > 0.83f && cur.xFraction in 0.30f..0.70f) {
                                                onDeleteItem(cur.id)
                                            }
                                            onDragStateChange(null, false)
                                        }
                                    }
                                }
                            }
                            .pointerInput(item.id) {
                                detectTapGestures(onTap = { onSelect(latestItem) })
                            }
                    } else Modifier
                )

            Box(modifier = baseModifier) {
                if (item.isEmoji) {
                    Text(
                        text = item.text,
                        fontSize = 46.sp,
                        modifier = Modifier
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .padding(4.dp)
                    )
                } else {
                    val textColor = when (item.style) {
                        StoryTextStyle.PILL -> item.color.textColor
                        StoryTextStyle.CLEAN, StoryTextStyle.NEON, StoryTextStyle.MONO -> {
                            if (item.color == StoryOverlayColor.BLACK) Color.White else item.color.bgColor.copy(alpha = 1f)
                        }
                    }
                    val align = when (item.align) {
                        StoryTextAlign.LEFT -> TextAlign.Start
                        StoryTextAlign.CENTER -> TextAlign.Center
                        StoryTextAlign.RIGHT -> TextAlign.End
                    }
                    val shadow = when (item.style) {
                        StoryTextStyle.CLEAN -> Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(0f, 3f), blurRadius = 10f)
                        StoryTextStyle.NEON -> Shadow(color = item.color.bgColor.copy(alpha = 1f), offset = Offset.Zero, blurRadius = 22f)
                        else -> null
                    }
                    val bgColor = when (item.style) {
                        StoryTextStyle.PILL -> item.color.bgColor
                        StoryTextStyle.MONO -> Color(0xCC0B0B0E)
                        else -> Color.Transparent
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = if (isSelected) BorderStroke(1.5.dp, InstaBlue) else null,
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        Text(
                            text = item.text,
                            color = textColor,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = if (item.style == StoryTextStyle.MONO) FontWeight.Medium else FontWeight.Bold,
                                fontFamily = if (item.style == StoryTextStyle.MONO) FontFamily.Monospace else FontFamily.Default,
                                shadow = shadow
                            ),
                            textAlign = align,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = InstaBlue,
                activeTrackColor = InstaBlue
            ),
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        )
    }
}

@Composable
private fun StoryToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) InstaBlue.copy(alpha = 0.24f) else Color(0xFF14141A),
        border = BorderStroke(0.75.dp, if (active) InstaBlue else Color(0xFF2D2D36)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (active) InstaBlue else Color.White, modifier = Modifier.size(16.dp))
            Text(label, color = if (active) InstaBlue else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LocalStoryVideoPreview(
    uri: Uri,
    muteAudio: Boolean,
    startMs: Long,
    endMs: Long,
    playbackSpeed: Float,
    paused: Boolean,
    onDurationDetected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(uri, startMs, endMs) {
        val builder = MediaItem.Builder().setUri(uri)
        if (endMs > startMs && endMs > 0L) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(endMs)
                    .build()
            )
        }
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(builder.build())
            repeatMode = Player.REPEAT_MODE_ALL
            volume = if (muteAudio) 0f else 1f
            setPlaybackSpeed(playbackSpeed.coerceIn(0.25f, 3f))
            prepare()
            playWhenReady = !paused
        }
    }

    LaunchedEffect(player, muteAudio) {
        player.volume = if (muteAudio) 0f else 1f
    }

    LaunchedEffect(player, playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed.coerceIn(0.25f, 3f))
    }

    LaunchedEffect(player, paused) {
        if (paused) player.pause() else player.play()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.duration > 0L) {
                    onDurationDetected(player.duration)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerDialog(
    storyGroups: List<StoryGroup>,
    initialAuthorId: String,
    currentUser: CampusUser,
    isAdmin: Boolean,
    usersById: Map<String, CampusUser>,
    onMarkViewed: (CampusStory) -> Unit,
    onToggleLikeStory: (CampusStory) -> Unit,
    onDeleteStory: (CampusStory) -> Unit,
    onReplyToStory: (story: CampusStory, authorName: String, replyText: String) -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onOpenAddStory: () -> Unit,
    onDismiss: () -> Unit
) {
    if (storyGroups.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val context = LocalContext.current

    var groupIndex by remember(initialAuthorId, storyGroups.size) {
        mutableIntStateOf(
            storyGroups.indexOfFirst { it.author.uid == initialAuthorId }.coerceAtLeast(0)
        )
    }
    val safeGroupIndex = groupIndex.coerceIn(0, storyGroups.lastIndex)
    val activeGroup = storyGroups[safeGroupIndex]

    var storyIndex by remember(activeGroup.author.uid) {
        val firstUnseen = activeGroup.stories.indexOfFirst {
            it.authorId != currentUser.uid && !it.viewedBy.contains(currentUser.uid)
        }
        mutableIntStateOf(if (firstUnseen >= 0) firstUnseen else 0)
    }
    if (activeGroup.stories.isEmpty()) {
        LaunchedEffect(activeGroup.stories.size) {
            if (groupIndex < storyGroups.lastIndex) groupIndex++ else onDismiss()
        }
        return
    }
    val safeStoryIndex = storyIndex.coerceIn(0, activeGroup.stories.lastIndex)
    val activeStory = activeGroup.stories[safeStoryIndex]

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember(activeStory.id) { mutableFloatStateOf(0f) }
    var replyText by remember(activeStory.id) { mutableStateOf("") }
    var showViewersSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val playbackActive = !isPaused && !showViewersSheet && !confirmDelete && replyText.isBlank()

    // Prefetch upcoming story songs in this author's group so transitions start instantly
    LaunchedEffect(activeGroup.author.uid, activeGroup.stories) {
        MusicCacheManager.prefetchUrls(
            context = context,
            urls = activeGroup.stories.mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
        )
    }

    // Dedicated ExoPlayer for story background music track
    val storyMusicUrl = activeStory.musicPreviewUrl?.takeIf { it.isNotBlank() }
    var isMusicBuffering by remember(activeStory.id, storyMusicUrl) {
        mutableStateOf(storyMusicUrl != null)
    }
    val storyMusicPlayer = remember(activeStory.id, storyMusicUrl) {
        storyMusicUrl?.let { url ->
            val resolvedUri = MusicCacheManager.resolvePlayableUri(context, url)
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(resolvedUri))
                repeatMode = Player.REPEAT_MODE_ALL
                if (activeStory.musicStartSec > 0) {
                    seekTo(activeStory.musicStartSec.coerceIn(0, 60) * 1000L)
                }
                prepare()
            }
        }
    }
    DisposableEffect(storyMusicPlayer) {
        val player = storyMusicPlayer
        if (player == null) {
            isMusicBuffering = false
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        isMusicBuffering = false
                    }
                }
            }
            if (player.playbackState == Player.STATE_READY) {
                isMusicBuffering = false
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
    }
    LaunchedEffect(storyMusicUrl, isMusicBuffering) {
        if (storyMusicUrl != null && isMusicBuffering) {
            delay(1800L)
            isMusicBuffering = false
        }
    }
    LaunchedEffect(storyMusicPlayer, activeStory.id, playbackActive) {
        val player = storyMusicPlayer ?: return@LaunchedEffect
        if (playbackActive) {
            player.playWhenReady = true
            player.play()
        } else {
            player.pause()
        }
    }

    val advanceNext: () -> Unit = {
        if (safeStoryIndex < activeGroup.stories.lastIndex) {
            storyIndex = safeStoryIndex + 1
        } else if (safeGroupIndex < storyGroups.lastIndex) {
            groupIndex = safeGroupIndex + 1
            storyIndex = 0
        } else {
            onDismiss()
        }
    }

    val goPrev: () -> Unit = {
        if (safeStoryIndex > 0) {
            storyIndex = safeStoryIndex - 1
        } else if (safeGroupIndex > 0) {
            val prevGroupIdx = safeGroupIndex - 1
            groupIndex = prevGroupIdx
            storyIndex = storyGroups[prevGroupIdx].stories.lastIndex.coerceAtLeast(0)
        } else {
            progress = 0f
        }
    }

    LaunchedEffect(activeStory.id) {
        onMarkViewed(activeStory)
    }

    LaunchedEffect(activeStory.id, playbackActive, isMusicBuffering) {
        val speed = activeStory.videoSpeed.coerceIn(0.25, 3.0)
        val durationMs = when {
            activeStory.mediaType == "video" -> {
                if (activeStory.videoEndMs > activeStory.videoStartMs) {
                    ((activeStory.videoEndMs - activeStory.videoStartMs) / speed).toLong().coerceIn(3_000L, 30_000L)
                } else {
                    (15_000.0 / speed).toLong().coerceIn(3_000L, 30_000L)
                }
            }
            !activeStory.musicPreviewUrl.isNullOrBlank() -> 15_000L
            else -> 6_000L
        }
        val tickMs = 50L
        while (progress < 1f) {
            if (playbackActive && !isMusicBuffering) {
                progress = (progress + tickMs.toFloat() / durationMs.toFloat()).coerceAtMost(1f)
            }
            delay(tickMs)
        }
        advanceNext()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Story Content + Tap Left/Right + Long-Press Pause
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeStory.id, safeGroupIndex, safeStoryIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                if (offset.x < size.width * 0.34f) {
                                    goPrev()
                                } else {
                                    advanceNext()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    activeStory.mediaType == "post_card" || activeStory.mediaUrl.startsWith("shared_text_post://") -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF101522),
                                            Color(0xFF0A0A0D),
                                            Color(0xFF1A1024)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            SharedPostStickerCard(
                                sourceType = activeStory.sharedSourceType ?: "post",
                                authorName = activeStory.sharedAuthorName ?: "Campus Member",
                                authorAvatar = activeStory.sharedAuthorAvatar,
                                sourceCaption = activeStory.sharedSourceCaption.orEmpty(),
                                onClickAuthor = activeStory.sharedAuthorId?.let { targetUid ->
                                    {
                                        onDismiss()
                                        onOpenAuthorProfile(targetUid)
                                    }
                                }
                            )
                        }
                    }

                    activeStory.mediaType == "video" || MediaUtils.isVideoMediaUri(activeStory.mediaUrl) -> {
                        val filterPreset = StoryFilterPreset.entries.firstOrNull { it.name == activeStory.videoFilter }
                            ?: StoryFilterPreset.ORIGINAL
                        val videoOverlays = remember(activeStory.id, activeStory.videoOverlaysJson) {
                            StoryOverlayItem.deserializeList(activeStory.videoOverlaysJson)
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            CampusVideoPlayer(
                                videoUri = activeStory.mediaUrl,
                                modifier = Modifier.fillMaxSize(),
                                autoPlay = true,
                                forceMuted = if (activeStory.muteOriginalAudio) true else null,
                                startMs = activeStory.videoStartMs,
                                endMs = activeStory.videoEndMs,
                                playbackSpeed = activeStory.videoSpeed.toFloat().coerceIn(0.25f, 3f),
                                paused = !playbackActive,
                                showControls = false,
                                showVideoBadge = false
                            )
                            val tint = filterPreset.videoTintColor()
                            if (tint != Color.Transparent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(tint)
                                )
                            }
                            if (videoOverlays.isNotEmpty()) {
                                StoryOverlayCanvas(
                                    overlays = videoOverlays,
                                    selectedId = null,
                                    interactive = false,
                                    onSelect = {},
                                    onUpdateItem = {},
                                    onDeleteItem = {},
                                    onDragStateChange = { _, _ -> },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    else -> {
                        CampusImage(
                            urlOrDataUri = activeStory.mediaUrl,
                            contentDescription = activeStory.caption.ifBlank { "${activeGroup.author.name} story" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )
            }

            // Segmented Progress Bars + Header + Music Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeGroup.stories.forEachIndexed { idx, _ ->
                        val segProgress = when {
                            idx < safeStoryIndex -> 1f
                            idx == safeStoryIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.34f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segProgress.coerceIn(0f, 1f))
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDismiss()
                                onOpenAuthorProfile(activeGroup.author.uid)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserAvatar(
                            src = activeGroup.author.avatar,
                            name = activeGroup.author.name,
                            role = activeGroup.author.role,
                            isVerified = activeGroup.author.isVerified,
                            isBlackSociety = activeGroup.author.isBlackSociety,
                            blackSocietyNumber = activeGroup.author.blackSocietyNumber,
                            isClassRepresentative = activeGroup.author.isClassRepresentative,
                            size = AvatarSize.SM
                        )
                        Text(
                            text = if (activeGroup.author.isBlackSociety) {
                                "Member - ${activeGroup.author.blackSocietyNumber ?: 1}"
                            } else {
                                activeGroup.author.name
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if ((activeGroup.author.isVerified || activeGroup.author.isClassRepresentative || activeGroup.author.role == "admin") && !activeGroup.author.isBlackSociety) {
                            VerifiedBadgeIcon(size = 14.dp)
                        }
                        Text(
                            text = formatRelativeTime(activeStory.createdAt),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeStory.muteOriginalAudio && activeStory.mediaType == "video") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Original video sound muted",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp)
                            )
                        }
                        if (activeGroup.isOwn) {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onOpenAddStory()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add another story", tint = Color.White)
                            }
                        }
                        if (activeStory.authorId == currentUser.uid || isAdmin) {
                            IconButton(
                                onClick = { confirmDelete = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete story", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                        }
                    }
                }

                if (!activeStory.musicPreviewUrl.isNullOrBlank() && !activeStory.musicTitle.isNullOrBlank()) {
                    StoryMusicStickerPill(
                        title = activeStory.musicTitle,
                        artist = activeStory.musicArtist ?: "",
                        artworkUrl = activeStory.musicArtworkUrl,
                        isPlaying = playbackActive
                    )
                }
            }

            // Bottom Repost Attribution + Caption + Reply / Viewers Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!activeStory.sharedSourceType.isNullOrBlank() && activeStory.mediaType != "post_card") {
                    val isReelShare = activeStory.sharedSourceType == "reel"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.68f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier.clickable(enabled = !activeStory.sharedAuthorId.isNullOrBlank()) {
                            activeStory.sharedAuthorId?.let { uid ->
                                onDismiss()
                                onOpenAuthorProfile(uid)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserAvatar(
                                    src = activeStory.sharedAuthorAvatar,
                                    name = activeStory.sharedAuthorName ?: "Campus Member",
                                    role = "student",
                                    isVerified = false,
                                    isBlackSociety = false,
                                    blackSocietyNumber = null,
                                    isClassRepresentative = false,
                                    size = AvatarSize.XS
                                )
                                Text(
                                    text = if (isReelShare) {
                                        "Reel by @${activeStory.sharedAuthorName ?: "Campus"}"
                                    } else {
                                        "Post by @${activeStory.sharedAuthorName ?: "Campus"}"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!activeStory.sharedSourceCaption.isNullOrBlank()) {
                                Text(
                                    text = activeStory.sharedSourceCaption,
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (activeStory.caption.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = activeStory.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (activeGroup.isOwn) {
                    val viewerCount = activeStory.viewedBy.size
                    val likeCount = activeStory.likedBy.size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { showViewersSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = "Viewers",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (viewerCount == 1) "Seen by 1 follower" else "Seen by $viewerCount followers",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (likeCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = InstaLikeRed.copy(alpha = 0.28f),
                                modifier = Modifier.clickable { showViewersSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Likes",
                                        tint = InstaLikeRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$likeCount",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val isLiked = activeStory.likedBy.contains(currentUser.uid)
                    val likeScale by animateFloatAsState(
                        targetValue = if (isLiked) 1.18f else 1f,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
                        label = "storyLikeScale"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text(
                                    "Reply to ${activeGroup.author.name.substringBefore(" ")}...",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.65f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                cursorColor = InstaBlue
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { onToggleLikeStory(activeStory) },
                            modifier = Modifier
                                .size(48.dp)
                                .scale(likeScale)
                                .clip(CircleShape)
                                .background(
                                    if (isLiked) InstaLikeRed.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f)
                                )
                                .testTag("story_like_button")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike story" else "Like story",
                                tint = if (isLiked) InstaLikeRed else Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onReplyToStory(
                                        activeStory,
                                        activeGroup.author.name,
                                        replyText
                                    )
                                    replyText = ""
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(InstaBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send story reply",
                                    tint = Color.White
                                )
                            }
                        } else {
                            listOf("🔥", "😂").forEach { emoji ->
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            onReplyToStory(
                                                activeStory,
                                                activeGroup.author.name,
                                                emoji
                                            )
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 17.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Delete Story?") },
            text = { Text("Remove this story for all your followers?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteStory(activeStory)
                        if (activeGroup.stories.size <= 1 && storyGroups.size <= 1) {
                            onDismiss()
                        } else {
                            advanceNext()
                        }
                    }
                ) {
                    Text("Delete", color = InstaLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showViewersSheet) {
        val viewerUsers = activeStory.viewedBy.mapNotNull { usersById[it] }
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Viewers (${activeStory.viewedBy.size}) · Likes (${activeStory.likedBy.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                if (viewerUsers.isEmpty()) {
                    Text(
                        text = "No followers have viewed this story yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(viewerUsers, key = { it.uid }) { viewer ->
                            val likedStory = activeStory.likedBy.contains(viewer.uid)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showViewersSheet = false
                                        onDismiss()
                                        onOpenAuthorProfile(viewer.uid)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UserAvatar(
                                    src = viewer.avatar,
                                    name = viewer.name,
                                    role = viewer.role,
                                    isVerified = viewer.isVerified,
                                    isBlackSociety = viewer.isBlackSociety,
                                    blackSocietyNumber = viewer.blackSocietyNumber,
                                    isClassRepresentative = viewer.isClassRepresentative,
                                    size = AvatarSize.SM
                                )
                                Text(
                                    text = if (viewer.isBlackSociety) "Member - ${viewer.blackSocietyNumber ?: 1}" else viewer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (likedStory) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Liked your story",
                                        tint = InstaLikeRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (viewer.isVerified && !viewer.isBlackSociety) {
                                    VerifiedBadgeIcon(size = 14.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedPostStickerCard(
    sourceType: String,
    authorName: String,
    authorAvatar: String?,
    sourceCaption: String,
    category: String? = null,
    onClickAuthor: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF16161C),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = if (onClickAuthor != null) {
                    Modifier.clickable(onClick = onClickAuthor)
                } else Modifier
            ) {
                UserAvatar(
                    src = authorAvatar,
                    name = authorName,
                    role = "student",
                    isVerified = false,
                    isBlackSociety = false,
                    blackSocietyNumber = null,
                    isClassRepresentative = false,
                    size = AvatarSize.SM
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(if (sourceType == "reel") "Campus Reel" else "Campus Post")
                            if (!category.isNullOrBlank()) append(" • $category")
                        },
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = InstaBlue.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = if (sourceType == "reel") "REEL" else "POST",
                        color = InstaBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (sourceCaption.isNotBlank()) {
                Text(
                    text = sourceCaption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 23.sp,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RepostToStoryDialog(
    sourceType: String, // "post" | "reel"
    authorName: String,
    authorAvatar: String?,
    previewMediaUrl: String?,
    sourceCaption: String,
    category: String,
    onShareToStory: (
        note: String,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        muteOriginalAudio: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedMusic by remember { mutableStateOf<StoryMusicTrack?>(null) }
    var musicStartSec by remember { mutableIntStateOf(0) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }

    val hasVideoPreview = sourceType == "reel" ||
        (!previewMediaUrl.isNullOrBlank() && MediaUtils.isVideoMediaUri(previewMediaUrl))

    val repostMusicPlayer = remember(selectedMusic?.previewUrl) {
        selectedMusic?.let { track ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(track.previewUrl))
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
            }
        }
    }
    DisposableEffect(repostMusicPlayer) {
        onDispose { repostMusicPlayer?.release() }
    }
    LaunchedEffect(repostMusicPlayer, musicStartSec, showMusicSheet, isSubmitting) {
        val player = repostMusicPlayer ?: return@LaunchedEffect
        if (showMusicSheet || isSubmitting) {
            player.pause()
        } else {
            player.seekTo(musicStartSec * 1000L)
            player.playWhenReady = true
            player.play()
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0E1320),
                            Color.Black,
                            Color(0xFF170F20)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (sourceType == "reel") "Add Reel to Story" else "Add Post to Story",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Original by $authorName",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                StoryToolButton(
                    icon = Icons.Filled.MusicNote,
                    label = if (selectedMusic != null) "Music ✓" else "Music",
                    active = selectedMusic != null,
                    onClick = { showMusicSheet = true },
                    testTag = "repost_music_button"
                )
            }

            // Center Instagram-style Repost Sticker Preview + Optional Music Pill
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 68.dp, bottom = 94.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                selectedMusic?.let { music ->
                    StoryMusicStickerPill(
                        title = music.title,
                        artist = music.artist,
                        artworkUrl = music.artworkUrl,
                        isPlaying = !showMusicSheet && !isSubmitting,
                        onClick = { showMusicSheet = true },
                        onRemove = {
                            selectedMusic = null
                            musicStartSec = 0
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (!previewMediaUrl.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF141418),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserAvatar(
                                    src = authorAvatar,
                                    name = authorName,
                                    role = "student",
                                    isVerified = false,
                                    isBlackSociety = false,
                                    blackSocietyNumber = null,
                                    isClassRepresentative = false,
                                    size = AvatarSize.SM
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = authorName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = category,
                                        color = Color.White.copy(alpha = 0.65f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Brush.linearGradient(listOf(InstaPurple, InstaPink, InstaOrange)).let { Color.White.copy(alpha = 0.14f) }
                                ) {
                                    Text(
                                        text = if (sourceType == "reel") "REEL" else "POST",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(310.dp)
                                    .background(Color.Black)
                            ) {
                                if (hasVideoPreview) {
                                    CampusVideoPlayer(
                                        videoUri = previewMediaUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        autoPlay = true,
                                        forceMuted = if (muteOriginalAudio) true else null,
                                        paused = showMusicSheet || isSubmitting
                                    )
                                } else {
                                    CampusImage(
                                        urlOrDataUri = previewMediaUrl,
                                        contentDescription = "Story preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            if (sourceCaption.isNotBlank()) {
                                Text(
                                    text = sourceCaption,
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                } else {
                    SharedPostStickerCard(
                        sourceType = sourceType,
                        authorName = authorName,
                        authorAvatar = authorAvatar,
                        sourceCaption = sourceCaption,
                        category = category
                    )
                }
            }

            // Bottom Note + Share Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    placeholder = {
                        Text(
                            "Add a thought (optional)...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.55f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                        cursorColor = InstaBlue
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            onShareToStory(note.trim(), selectedMusic, musicStartSec, hasVideoPreview && muteOriginalAudio) {
                                isSubmitting = false
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_repost_to_story_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isSubmitting) "Sharing..." else "Your Story", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showMusicSheet) {
        StoryMusicPickerSheet(
            currentTrack = selectedMusic,
            currentStartSec = musicStartSec,
            allowMuteVideoAudio = hasVideoPreview,
            muteOriginalAudio = muteOriginalAudio,
            onApply = { track, startSec, muteOriginal ->
                selectedMusic = track
                musicStartSec = startSec
                muteOriginalAudio = if (hasVideoPreview) muteOriginal else false
            },
            onDismiss = { showMusicSheet = false }
        )
    }
}
