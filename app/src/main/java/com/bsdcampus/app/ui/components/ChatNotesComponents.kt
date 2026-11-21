package com.bsdcampus.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusNote
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.NoteBubbleColor
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import kotlinx.coroutines.delay

private data class NoteCountdownUiState(
    val fraction: Float,
    val compactLabel: String,
    val detailedLabel: String,
    val isExpiringSoon: Boolean
)

@Composable
private fun rememberNoteCountdownState(note: CampusNote?): NoteCountdownUiState? {
    if (note == null) return null
    var nowMs by remember(note.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(note.id, note.expiresAt) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val defaultTtlMs = 24L * 60L * 60L * 1000L
    val expiresAtMs = note.expiresAt?.toDate()?.time
    if (expiresAtMs == null) {
        return NoteCountdownUiState(
            fraction = 1f,
            compactLabel = "24h",
            detailedLabel = "24h 0m left",
            isExpiringSoon = false
        )
    }
    val createdAtMs = note.createdAt?.toDate()?.time ?: (expiresAtMs - defaultTtlMs)
    val totalMs = (expiresAtMs - createdAtMs).coerceAtLeast(60_000L)
    val remainingMs = (expiresAtMs - nowMs).coerceAtLeast(0L)
    val fraction = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    val compactLabel = when {
        remainingMs <= 0L -> "0m"
        hours >= 1L -> "${hours}h"
        totalMinutes >= 1L -> "${totalMinutes}m"
        else -> "<1m"
    }
    val detailedLabel = when {
        remainingMs <= 0L -> "Expiring now"
        hours >= 1L -> "${hours}h ${mins}m left"
        totalMinutes >= 1L -> "${totalMinutes}m left"
        else -> "Less than 1m left"
    }
    return NoteCountdownUiState(
        fraction = fraction,
        compactLabel = compactLabel,
        detailedLabel = detailedLabel,
        isExpiringSoon = remainingMs in 1L..(60L * 60L * 1000L)
    )
}

private fun bubbleProgressAccentColor(palette: NoteBubbleColor, isExpiringSoon: Boolean): Color {
    if (isExpiringSoon) return InstaLikeRed
    return when (palette) {
        NoteBubbleColor.PINK -> Color(0xFFE1306CL)
        NoteBubbleColor.DARK_PINK -> Color(0xFFFBCFE8L)
        NoteBubbleColor.BLUE -> Color(0xFFBAE6FDL)
        NoteBubbleColor.PURPLE -> Color(0xFFE9D5FFL)
        NoteBubbleColor.EMERALD -> Color(0xFF5EEAD4L)
        NoteBubbleColor.AMBER -> Color(0xFF78350FL)
        NoteBubbleColor.DARK -> InstaBlue
    }
}

private fun buildClockwiseRoundedRectPath(
    width: Float,
    height: Float,
    cornerRadiusPx: Float,
    insetPx: Float
): Path {
    val left = insetPx
    val top = insetPx
    val right = (width - insetPx).coerceAtLeast(left + 1f)
    val bottom = (height - insetPx).coerceAtLeast(top + 1f)
    val r = cornerRadiusPx.coerceIn(1f, minOf((right - left) / 2f, (bottom - top) / 2f))
    val d = r * 2f

    return Path().apply {
        moveTo(width / 2f, top)
        lineTo(right - r, top)
        arcTo(Rect(right - d, top, right, top + d), startAngleDegrees = -90f, sweepAngleDegrees = 90f, forceMoveTo = false)
        lineTo(right, bottom - r)
        arcTo(Rect(right - d, bottom - d, right, bottom), startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false)
        lineTo(left + r, bottom)
        arcTo(Rect(left, bottom - d, left + d, bottom), startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false)
        lineTo(left, top + r)
        arcTo(Rect(left, top, left + d, top + d), startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false)
        close()
    }
}

private fun Modifier.drawStatusBubbleProgressRing(
    fraction: Float,
    cornerRadius: Dp,
    strokeWidth: Dp,
    trackColor: Color,
    progressColor: Color
): Modifier = drawWithContent {
    drawContent()
    val strokePx = strokeWidth.toPx()
    val fullPath = buildClockwiseRoundedRectPath(
        width = size.width,
        height = size.height,
        cornerRadiusPx = cornerRadius.toPx(),
        insetPx = strokePx / 2f
    )
    drawPath(
        path = fullPath,
        color = trackColor,
        style = Stroke(width = strokePx)
    )
    val clamped = fraction.coerceIn(0f, 1f)
    if (clamped > 0f) {
        val measure = PathMeasure().apply { setPath(fullPath, false) }
        val activePath = Path()
        measure.getSegment(0f, measure.length * clamped, activePath, true)
        drawPath(
            path = activePath,
            color = progressColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun NoteAvatarItem(
    user: CampusUser,
    note: CampusNote?,
    isOwn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = note?.let { NoteBubbleColor.fromKey(it.bubbleColor) }
    val hasNote = note != null && (note.text.isNotBlank() || !note.musicTitle.isNullOrBlank())
    val countdown = rememberNoteCountdownState(if (hasNote) note else null)

    val bubbleContainer = if (hasNote && palette != null) {
        Color(palette.containerArgb)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleContent = if (hasNote && palette != null) {
        Color(palette.textArgb)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bubbleBorder = if (hasNote && palette != null) {
        Color(palette.borderArgb).copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    }
    val progressAccent = if (hasNote && palette != null && countdown != null) {
        bubbleProgressAccentColor(palette, countdown.isExpiringSoon)
    } else {
        bubbleBorder
    }

    val displayName = when {
        isOwn -> "Your note"
        user.isBlackSociety -> "M-#${user.blackSocietyNumber ?: 1}"
        else -> user.name.substringBefore(" ")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag(if (isOwn) "chat_own_note_item" else "chat_note_item_${user.uid}")
    ) {
        // Fixed-height thought bubble zone so every avatar aligns along a shared baseline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (hasNote && note != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 3.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bubbleContainer,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .widthIn(min = 52.dp, max = 82.dp)
                                .drawStatusBubbleProgressRing(
                                    fraction = countdown?.fraction ?: 1f,
                                    cornerRadius = 16.dp,
                                    strokeWidth = 2.25.dp,
                                    trackColor = bubbleContent.copy(alpha = 0.2f),
                                    progressColor = progressAccent
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (note.text.isNotBlank()) {
                                    Text(
                                        text = note.text,
                                        color = bubbleContent,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = if (!note.musicTitle.isNullOrBlank()) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (!note.musicTitle.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = if (note.text.isNotBlank()) 1.dp else 0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = bubbleContent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = note.musicTitle,
                                            color = bubbleContent.copy(alpha = 0.92f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (countdown != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, progressAccent),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-6).dp)
                                    .testTag(if (isOwn) "own_note_timer_badge" else "note_timer_badge_${user.uid}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Time remaining",
                                        tint = if (countdown.isExpiringSoon) InstaLikeRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = countdown.compactLabel,
                                        color = if (countdown.isExpiringSoon) InstaLikeRed else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    // Thought-bubble trail connecting to the top of the avatar
                    Box(
                        modifier = Modifier
                            .offset(x = (-10).dp, y = (-1).dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(0.75.dp, progressAccent, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = (-14).dp, y = 0.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(0.5.dp, progressAccent, CircleShape)
                    )
                }
            } else if (isOwn) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bubbleContainer,
                        border = BorderStroke(0.75.dp, bubbleBorder),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = "Note...",
                            color = bubbleContent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = (-9).dp, y = (-1).dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(0.6.dp, bubbleBorder, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = (-13).dp, y = 0.dp)
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(0.5.dp, bubbleBorder, CircleShape)
                    )
                }
            }
        }

        Box {
            UserAvatar(
                src = user.avatar,
                name = user.name,
                role = user.role,
                isVerified = user.isVerified,
                isBlackSociety = user.isBlackSociety,
                blackSocietyNumber = user.blackSocietyNumber,
                isClassRepresentative = user.isClassRepresentative,
                showStoryRing = user.role == "student" && !user.isClassRepresentative && !user.isBlackSociety,
                size = AvatarSize.LG
            )
            if (isOwn && !hasNote) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(InstaBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add note",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (hasNote) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isOwn && !hasNote) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NoteComposerDialog(
    currentUser: CampusUser,
    existingNote: CampusNote?,
    onSave: (text: String, bubbleColor: NoteBubbleColor, track: StoryMusicTrack?, startSec: Int, onDone: () -> Unit) -> Unit,
    onDelete: (onDone: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember(existingNote) { mutableStateOf(existingNote?.text.orEmpty()) }
    var selectedColor by remember(existingNote) {
        mutableStateOf(NoteBubbleColor.fromKey(existingNote?.bubbleColor))
    }
    var selectedTrack by remember(existingNote) {
        mutableStateOf(
            existingNote?.musicPreviewUrl?.takeIf { it.isNotBlank() }?.let { previewUrl ->
                StoryMusicTrack(
                    trackId = existingNote.id.hashCode().toLong(),
                    title = existingNote.musicTitle.orEmpty(),
                    artist = existingNote.musicArtist.orEmpty(),
                    previewUrl = previewUrl,
                    artworkUrl = existingNote.musicArtworkUrl.orEmpty(),
                    durationMs = 30_000L
                )
            }
        )
    }
    var musicStartSec by remember(existingNote) { mutableIntStateOf(existingNote?.musicStartSec ?: 0) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val previewNote = remember(currentUser.uid, noteText, selectedColor, selectedTrack, musicStartSec) {
        if (noteText.isBlank() && selectedTrack == null) {
            null
        } else {
            CampusNote(
                id = currentUser.uid,
                authorId = currentUser.uid,
                authorName = currentUser.name,
                authorAvatar = currentUser.avatar,
                text = noteText.trim(),
                bubbleColor = selectedColor.key,
                musicTitle = selectedTrack?.title,
                musicArtist = selectedTrack?.artist,
                musicArtworkUrl = selectedTrack?.artworkUrl,
                musicPreviewUrl = selectedTrack?.previewUrl,
                musicStartSec = musicStartSec
            )
        }
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (existingNote != null) "Update Note" else "New Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Visible to mutual followers for 24h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close note composer")
                    }
                }

                // Live Thought Bubble Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NoteAvatarItem(
                        user = currentUser,
                        note = previewNote,
                        isOwn = true,
                        onClick = {}
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 100) noteText = it },
                    label = { Text("Share what's on your mind...") },
                    supportingText = { Text("${noteText.length}/100") },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_text_input")
                )

                // Bubble Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bubble Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedColor.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoteBubbleColor.entries.forEach { palette ->
                            val isSelected = selectedColor == palette
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedColor = palette }
                                    .testTag("note_color_${palette.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(palette.containerArgb))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color(palette.borderArgb),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "${palette.label} selected",
                                            tint = Color(palette.textArgb),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Song Attachment Row
                if (selectedTrack != null) {
                    val track = selectedTrack!!
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMusicPicker = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!track.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(InstaBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = InstaBlue)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { selectedTrack = null },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove song")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showMusicPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("note_add_song_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = InstaBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Song to Note")
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingNote != null) {
                        TextButton(
                            onClick = {
                                isSaving = true
                                onDelete {
                                    isSaving = false
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("note_delete_button")
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = InstaLikeRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = InstaLikeRed)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                isSaving = true
                                onSave(noteText.trim(), selectedColor, selectedTrack, musicStartSec) {
                                    isSaving = false
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving && (noteText.isNotBlank() || selectedTrack != null),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                            modifier = Modifier.testTag("note_share_button")
                        ) {
                            Text(if (isSaving) "Sharing..." else "Share")
                        }
                    }
                }
            }
        }
    }

    if (showMusicPicker) {
        StoryMusicPickerSheet(
            currentTrack = selectedTrack,
            currentStartSec = musicStartSec,
            allowMuteVideoAudio = false,
            muteOriginalAudio = false,
            onApply = { track, startSec, _ ->
                selectedTrack = track
                musicStartSec = startSec
                showMusicPicker = false
            },
            onDismiss = { showMusicPicker = false }
        )
    }
}

@Composable
fun NoteViewerDialog(
    note: CampusNote,
    author: CampusUser?,
    isOwnNote: Boolean,
    onEditOwnNote: () -> Unit,
    onDeleteOwnNote: () -> Unit,
    onQuickReply: (String) -> Unit,
    onOpenChat: () -> Unit,
    onOpenProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = NoteBubbleColor.fromKey(note.bubbleColor)
    val countdown = rememberNoteCountdownState(note)
    val bubbleContainer = Color(palette.containerArgb)
    val bubbleText = Color(palette.textArgb)
    val progressAccent = bubbleProgressAccentColor(palette, countdown?.isExpiringSoon == true)

    var replyDraft by remember { mutableStateOf("") }
    val hasMusic = !note.musicPreviewUrl.isNullOrBlank() && !note.musicTitle.isNullOrBlank()
    var isPlaying by remember(note.id, note.musicPreviewUrl) { mutableStateOf(hasMusic) }

    val exoPlayer = remember(note.id, note.musicPreviewUrl) {
        if (hasMusic) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                setMediaItem(MediaItem.fromUri(note.musicPreviewUrl!!))
                prepare()
                if (note.musicStartSec > 0) {
                    seekTo(note.musicStartSec * 1000L)
                }
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    val eqTransition = rememberInfiniteTransition(label = "noteMusicEq")
    val eqPulse by eqTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "eqPulse"
    )

    val authorName = when {
        author?.isBlackSociety == true -> "Member - ${author.blackSocietyNumber ?: 1}"
        author != null -> author.name
        else -> note.authorName
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDismiss()
                                onOpenProfile()
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (author?.isVerified == true && !author.isBlackSociety) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadgeIcon(size = 14.dp)
                            }
                        }
                        Text(
                            text = buildString {
                                append("Shared ${formatRelativeTime(note.createdAt)}")
                                countdown?.let { append(" • ${it.detailedLabel}") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (countdown?.isExpiringSoon == true) InstaLikeRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close note")
                    }
                }

                if (countdown != null) {
                    LinearProgressIndicator(
                        progress = { countdown.fraction },
                        color = if (countdown.isExpiringSoon) InstaLikeRed else InstaBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .testTag("note_viewer_countdown_bar")
                    )
                }

                // Expanded Thought Bubble + Progress Ring + Connecting Dots + Avatar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = bubbleContainer,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 250.dp)
                                .drawStatusBubbleProgressRing(
                                    fraction = countdown?.fraction ?: 1f,
                                    cornerRadius = 22.dp,
                                    strokeWidth = 2.75.dp,
                                    trackColor = bubbleText.copy(alpha = 0.2f),
                                    progressColor = progressAccent
                                )
                        ) {
                            Text(
                                text = note.text.ifBlank { "♪ ${note.musicTitle.orEmpty()}" },
                                color = bubbleText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        if (countdown != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.9.dp, progressAccent),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-8).dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Time left",
                                        tint = if (countdown.isExpiringSoon) InstaLikeRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = countdown.compactLabel,
                                        color = if (countdown.isExpiringSoon) InstaLikeRed else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = (-18).dp, y = (-1).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(1.dp, progressAccent, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = (-24).dp, y = 1.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(bubbleContainer)
                            .border(0.75.dp, progressAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    UserAvatar(
                        src = author?.avatar ?: note.authorAvatar,
                        name = authorName,
                        role = author?.role ?: "student",
                        isVerified = author?.isVerified == true,
                        isBlackSociety = author?.isBlackSociety == true,
                        blackSocietyNumber = author?.blackSocietyNumber,
                        isClassRepresentative = author?.isClassRepresentative == true,
                        size = AvatarSize.XL
                    )
                }

                // Music Card if Note has Song
                if (hasMusic) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!note.musicArtworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = note.musicArtworkUrl,
                                    contentDescription = note.musicTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(InstaBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = InstaBlue)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isPlaying) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = InstaBlue.copy(alpha = eqPulse),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = note.musicTitle.orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!note.musicArtist.isNullOrBlank()) {
                                    Text(
                                        text = note.musicArtist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    exoPlayer?.let { p ->
                                        if (p.isPlaying) p.pause() else p.play()
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("note_viewer_music_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause song" else "Play song",
                                    tint = InstaBlue
                                )
                            }
                        }
                    }
                }

                if (isOwnNote) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeleteOwnNote,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("note_viewer_delete_button")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = InstaLikeRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = InstaLikeRed)
                        }
                        Button(
                            onClick = onEditOwnNote,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("note_viewer_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Note")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyDraft,
                            onValueChange = { replyDraft = it },
                            placeholder = { Text("Send reply...") },
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("note_reply_input")
                        )
                        IconButton(
                            onClick = {
                                val trimmed = replyDraft.trim()
                                if (trimmed.isNotBlank()) {
                                    onQuickReply(trimmed)
                                    replyDraft = ""
                                }
                            },
                            enabled = replyDraft.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (replyDraft.isNotBlank()) InstaBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("note_reply_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Send note reply",
                                tint = if (replyDraft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenProfile()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Profile")
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenChat()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}
