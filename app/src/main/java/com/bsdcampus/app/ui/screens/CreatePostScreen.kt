package com.bsdcampus.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.StoryEditParams
import com.bsdcampus.app.data.StoryFilterPreset
import com.bsdcampus.app.data.StoryMediaEditor
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.PostImageRatio
import com.bsdcampus.app.notifications.StagedDraftMediaInput
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.StoryMusicPickerSheet
import com.bsdcampus.app.ui.components.StoryMusicStickerPill
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.theme.InstaBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    onPublish: (
        content: String,
        category: String,
        mediaItems: List<StagedDraftMediaInput>,
        imageAspectRatio: PostImageRatio,
        isOfficial: Boolean,
        isPinned: Boolean,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        onDone: () -> Unit
    ) -> Unit,
    onPublishReel: ((
        videoUri: Uri,
        caption: String,
        category: String,
        audioTitle: String,
        videoAspectRatio: PostImageRatio,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        muteOriginalAudio: Boolean,
        onDone: () -> Unit
    ) -> Unit)? = null,
    onShowToast: (String) -> Unit = {}
) {
    val context = LocalContext.current

    var isReelMode by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("") }
    var reelVideoUri by remember { mutableStateOf<Uri?>(null) }
    var reelAspectRatio by remember { mutableStateOf(PostImageRatio.FREE) }
    var selectedCategory by remember { mutableStateOf("General") }
    var selectedAspectRatio by remember { mutableStateOf(PostImageRatio.FREE) }
    var isOfficial by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedMusicTrack by remember { mutableStateOf<StoryMusicTrack?>(null) }
    var selectedMusicStartSec by remember { mutableIntStateOf(0) }
    var muteReelOriginalAudio by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var editingPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val selectedMedia = remember { mutableStateListOf<StagedDraftMediaInput>() }

    val pickReelVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            reelVideoUri = uri
        }
    }

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val isVideo = MediaUtils.isVideoContentUri(context, uri)
                selectedMedia.add(
                    StagedDraftMediaInput(
                        uri = uri,
                        isVideo = isVideo
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp)
        ) {
            // Author Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    src = currentUser.avatar,
                    name = currentUser.name,
                    role = currentUser.role,
                    isVerified = currentUser.isVerified,
                    isBlackSociety = currentUser.isBlackSociety,
                    blackSocietyNumber = currentUser.blackSocietyNumber,
                    isClassRepresentative = currentUser.isClassRepresentative,
                    size = AvatarSize.MD
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentUser.isBlackSociety) "Member - ${currentUser.blackSocietyNumber ?: 1}" else currentUser.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentUser.isVerified && !currentUser.isBlackSociety) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadgeIcon(size = 14.dp)
                        }
                    }
                    Text(
                        text = buildString {
                            append(currentUser.grade ?: currentUser.role.replaceFirstChar { it.uppercase() })
                            append(" • Unlimited Photos & Videos")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            if (onPublishReel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isReelMode,
                        onClick = { isReelMode = false },
                        label = { Text("Post", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                            selectedLabelColor = InstaBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_mode_post")
                    )
                    FilterChip(
                        selected = isReelMode,
                        onClick = { isReelMode = true },
                        label = { Text("Reel", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                            selectedLabelColor = InstaBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_mode_reel")
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 1000) caption = it },
                    placeholder = {
                        Text(
                            if (isReelMode) "Write a reel caption or campus highlights..."
                            else "Write a caption, announcement, or campus update..."
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_caption")
                )

                if (isReelMode) {
                    OutlinedTextField(
                        value = audioTitle,
                        onValueChange = { audioTitle = it.take(120) },
                        label = { Text("Audio name (optional)") },
                        placeholder = { Text("Original audio") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isReelMode) {
                            "Reel mode: unlimited video size & background upload"
                        } else {
                            "Unlimited photos & videos • background upload"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${caption.length}/1000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Media Picker Buttons & Previews
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isReelMode) {
                        OutlinedButton(
                            onClick = {
                                pickReelVideo.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_reel_video_button")
                        ) {
                            Icon(Icons.Outlined.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (reelVideoUri == null) "Pick Reel Video" else "Video Selected • Change Video",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        reelVideoUri?.let { pickedUri ->
                            val naturalReelRatio by produceState(initialValue = 9f / 16f, key1 = pickedUri) {
                                value = StoryMediaEditor.readVideoAspectRatio(context, pickedUri)
                            }
                            val reelThumb by produceState<Bitmap?>(initialValue = null, key1 = pickedUri) {
                                value = StoryMediaEditor.extractVideoThumbnailBitmap(context, pickedUri)
                            }
                            val previewRatio = (reelAspectRatio.ratio ?: naturalReelRatio).coerceIn(0.45f, 1.91f)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Reel Aspect Ratio",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PostImageRatio.entries.forEach { ratio ->
                                        FilterChip(
                                            selected = reelAspectRatio == ratio,
                                            onClick = { reelAspectRatio = ratio },
                                            label = { Text(ratio.label, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                                                selectedLabelColor = InstaBlue
                                            ),
                                            modifier = Modifier.testTag("reel_ratio_${ratio.key}")
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .height(176.dp)
                                        .aspectRatio(previewRatio)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF121216)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbBmp = reelThumb
                                    if (thumbBmp != null) {
                                        Image(
                                            bitmap = thumbBmp.asImageBitmap(),
                                            contentDescription = "Selected reel preview",
                                            contentScale = if (reelAspectRatio == PostImageRatio.FREE) ContentScale.Fit else ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(InstaBlue.copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Selected reel video", tint = Color.White)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.72f)
                                    ) {
                                        Text(
                                            text = reelAspectRatio.label,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { reelVideoUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove reel video", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                pickMultipleMedia.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_post_photos_button")
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Outlined.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Photos & Videos (${selectedMedia.size} • Unlimited)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedMedia.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Post Aspect Ratio",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PostImageRatio.entries.forEach { ratio ->
                                        FilterChip(
                                            selected = selectedAspectRatio == ratio,
                                            onClick = { selectedAspectRatio = ratio },
                                            label = { Text(ratio.label, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                                                selectedLabelColor = InstaBlue
                                            ),
                                            modifier = Modifier.testTag("post_ratio_${ratio.key}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isReelMode && selectedMedia.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(selectedMedia, key = { _, item -> item.id }) { idx, item ->
                                val itemNaturalRatio by produceState(
                                    initialValue = 1f,
                                    key1 = item.uri,
                                    key2 = item.editParams.rotationDegrees
                                ) {
                                    value = if (item.isVideo) {
                                        StoryMediaEditor.readVideoAspectRatio(context, item.uri)
                                    } else {
                                        StoryMediaEditor.readImageAspectRatio(context, item.uri, item.editParams.rotationDegrees)
                                    }
                                }
                                val itemPreviewRatio = (selectedAspectRatio.ratio ?: itemNaturalRatio).coerceIn(0.45f, 1.91f)
                                val videoThumb by produceState<Bitmap?>(initialValue = null, key1 = item.uri, key2 = item.isVideo) {
                                    value = if (item.isVideo) {
                                        StoryMediaEditor.extractVideoThumbnailBitmap(context, item.uri)
                                    } else null
                                }
                                Box(
                                    modifier = Modifier
                                        .height(136.dp)
                                        .aspectRatio(itemPreviewRatio)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (item.isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF15151B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val thumbBmp = videoThumb
                                            if (thumbBmp != null) {
                                                Image(
                                                    bitmap = thumbBmp.asImageBitmap(),
                                                    contentDescription = "Selected video ${idx + 1}",
                                                    contentScale = if (selectedAspectRatio == PostImageRatio.FREE) ContentScale.Fit else ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Selected video ${idx + 1}",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        val edit = item.editParams
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = "Selected photo ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            colorFilter = ColorFilter.colorMatrix(
                                                edit.filter.composeColorMatrix(
                                                    brightness = edit.brightness,
                                                    contrast = edit.contrast,
                                                    saturation = edit.saturation
                                                )
                                            ),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    rotationZ = edit.rotationDegrees.toFloat()
                                                    scaleX = if (edit.flipHorizontal) -1f else 1f
                                                }
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.68f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (item.isVideo) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                            Text(
                                                text = if (item.isVideo) "VID #${idx + 1}" else "#${idx + 1}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (!item.isVideo) {
                                        Surface(
                                            onClick = { editingPhotoIndex = idx },
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.72f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .testTag("edit_post_photo_$idx")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit photo ${idx + 1}",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Edit",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { selectedMedia.removeAt(idx) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove media",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Song / Music Attachment (for both Posts and Reels)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Music", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    val attachedTrack = selectedMusicTrack
                    if (attachedTrack == null) {
                        OutlinedButton(
                            onClick = { showMusicPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("create_add_music_button")
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isReelMode) "Add Song to Reel" else "Add Song to Post",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        StoryMusicStickerPill(
                            title = attachedTrack.title,
                            artist = buildString {
                                append(attachedTrack.artist)
                                if (selectedMusicStartSec > 0) append(" · Starts ${selectedMusicStartSec}s")
                                if (isReelMode && muteReelOriginalAudio) append(" · Video audio muted")
                            },
                            artworkUrl = attachedTrack.artworkUrl,
                            isPlaying = false,
                            onClick = { showMusicPicker = true },
                            onRemove = {
                                selectedMusicTrack = null
                                selectedMusicStartSec = 0
                                muteReelOriginalAudio = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CampusConstants.POST_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                                    selectedLabelColor = InstaBlue
                                )
                            )
                        }
                    }
                }

                // Admin Official / Pin Controls (Posts only)
                if (isAdmin && !isReelMode) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = InstaBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Official School Notice", style = MaterialTheme.typography.labelLarge)
                                }
                                Switch(checked = isOfficial, onCheckedChange = { isOfficial = it })
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.PushPin, contentDescription = null, tint = InstaBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pin to Top of Feed", style = MaterialTheme.typography.labelLarge)
                                }
                                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isReelMode && onPublishReel != null) {
                            val readyUri = reelVideoUri ?: return@Button
                            onPublishReel(
                                readyUri,
                                caption.trim(),
                                selectedCategory,
                                audioTitle.trim().ifBlank { "Original audio" },
                                reelAspectRatio,
                                selectedMusicTrack,
                                selectedMusicStartSec,
                                muteReelOriginalAudio
                            ) {
                                caption = ""
                                audioTitle = ""
                                reelVideoUri = null
                                reelAspectRatio = PostImageRatio.FREE
                                selectedMusicTrack = null
                                selectedMusicStartSec = 0
                                muteReelOriginalAudio = false
                            }
                        } else if (caption.isNotBlank() || selectedMedia.isNotEmpty()) {
                            onPublish(
                                caption.trim(),
                                selectedCategory,
                                selectedMedia.toList(),
                                selectedAspectRatio,
                                isOfficial,
                                isPinned,
                                selectedMusicTrack,
                                selectedMusicStartSec
                            ) {
                                caption = ""
                                selectedMedia.clear()
                                selectedAspectRatio = PostImageRatio.FREE
                                isOfficial = false
                                isPinned = false
                                selectedMusicTrack = null
                                selectedMusicStartSec = 0
                            }
                        }
                    },
                    enabled = if (isReelMode) {
                        reelVideoUri != null
                    } else {
                        caption.isNotBlank() || selectedMedia.isNotEmpty()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("publish_post_button")
                ) {
                    Text(if (isReelMode) "Share Reel" else "Share to Squadify", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showMusicPicker) {
        StoryMusicPickerSheet(
            title = if (isReelMode) "Add Music to Reel" else "Add Music to Post",
            currentTrack = selectedMusicTrack,
            currentStartSec = selectedMusicStartSec,
            allowMuteVideoAudio = isReelMode,
            muteOriginalAudio = muteReelOriginalAudio,
            onApply = { track, startSec, muteOriginal ->
                selectedMusicTrack = track
                selectedMusicStartSec = startSec
                muteReelOriginalAudio = muteOriginal
            },
            onDismiss = { showMusicPicker = false }
        )
    }

    editingPhotoIndex?.let { idx ->
        val item = selectedMedia.getOrNull(idx)
        if (item != null && !item.isVideo) {
            BasicPhotoEditorDialog(
                uri = item.uri,
                initialEdit = item.editParams,
                targetRatio = selectedAspectRatio,
                onSave = { updatedEdit ->
                    if (idx in selectedMedia.indices) {
                        selectedMedia[idx] = item.copy(editParams = updatedEdit)
                    }
                    editingPhotoIndex = null
                },
                onDismiss = { editingPhotoIndex = null }
            )
        } else {
            LaunchedEffect(Unit) { editingPhotoIndex = null }
        }
    }
}

@Composable
private fun BasicPhotoEditorDialog(
    uri: Uri,
    initialEdit: StoryEditParams,
    targetRatio: PostImageRatio,
    onSave: (StoryEditParams) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var draftEdit by remember(uri, initialEdit) { mutableStateOf(initialEdit) }
    var naturalRatio by remember(uri) { mutableFloatStateOf(1f) }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(uri, draftEdit.rotationDegrees) {
        naturalRatio = StoryMediaEditor.readImageAspectRatio(context, uri, draftEdit.rotationDegrees)
    }

    val previewRatio = (targetRatio.ratio ?: naturalRatio).coerceIn(0.55f, 1.95f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C0C0F),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Text(
                        text = "Edit Photo (${targetRatio.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(
                        onClick = { onSave(draftEdit) },
                        modifier = Modifier.testTag("photo_editor_apply")
                    ) {
                        Text("Done", color = InstaBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(previewRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Edited photo preview",
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(
                                draftEdit.filter.composeColorMatrix(
                                    brightness = draftEdit.brightness,
                                    contrast = draftEdit.contrast,
                                    saturation = draftEdit.saturation
                                )
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = draftEdit.rotationDegrees.toFloat()
                                    scaleX = if (draftEdit.flipHorizontal) -1f else 1f
                                }
                        )
                    }
                }

                // Quick transform & reset controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            draftEdit = draftEdit.copy(rotationDegrees = (draftEdit.rotationDegrees + 90) % 360)
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("photo_editor_rotate")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            draftEdit = draftEdit.copy(flipHorizontal = !draftEdit.flipHorizontal)
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("photo_editor_flip")
                    ) {
                        Icon(Icons.Filled.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flip", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { draftEdit = StoryEditParams() },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("photo_editor_reset")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", color = Color.White)
                    }
                }

                // Filter Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryFilterPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = draftEdit.filter == preset,
                            onClick = { draftEdit = draftEdit.copy(filter = preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InstaBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("photo_filter_${preset.name}")
                        )
                    }
                }

                // Brightness & Contrast Sliders
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text("${(draftEdit.brightness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InstaBlue)
                    }
                    Slider(
                        value = draftEdit.brightness,
                        onValueChange = { draftEdit = draftEdit.copy(brightness = it) },
                        valueRange = -0.4f..0.4f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = InstaBlue)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Contrast", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text("${(draftEdit.contrast * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InstaBlue)
                    }
                    Slider(
                        value = draftEdit.contrast,
                        onValueChange = { draftEdit = draftEdit.copy(contrast = it) },
                        valueRange = 0.5f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = InstaBlue)
                    )
                }
            }
        }
    }
}
