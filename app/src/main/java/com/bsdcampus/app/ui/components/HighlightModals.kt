package com.bsdcampus.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.model.CampusStory
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.HighlightMediaItem
import com.bsdcampus.app.model.StoryHighlight
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HighlightCreatorDialog(
    currentUser: CampusUser,
    isAdmin: Boolean,
    ownActiveStories: List<CampusStory>,
    existingHighlight: StoryHighlight? = null,
    onShowToast: (String) -> Unit,
    onSave: (title: String, coverAvatar: String?, coverEmoji: String?, items: List<HighlightMediaItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(existingHighlight?.id) { mutableStateOf(existingHighlight?.title.orEmpty()) }
    var customCoverAvatar by remember(existingHighlight?.id) { mutableStateOf(existingHighlight?.coverAvatar) }
    var selectedEmoji by remember(existingHighlight?.id) { mutableStateOf(existingHighlight?.coverEmoji) }
    val items = remember(existingHighlight?.id) {
        mutableStateListOf<HighlightMediaItem>().apply {
            if (existingHighlight != null) {
                addAll(existingHighlight.items)
            }
        }
    }
    var isUploadingCover by remember { mutableStateOf(false) }
    var isUploadingMedia by remember { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingCover = true
            scope.launch {
                CampusMediaRepository.uploadPickedMediaUri(
                    context = context,
                    uri = uri,
                    isAdmin = isAdmin
                ).onSuccess { uploaded ->
                    customCoverAvatar = uploaded
                }.onFailure {
                    onShowToast(it.localizedMessage ?: "Unable to upload highlight cover")
                }
                isUploadingCover = false
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            isUploadingMedia = true
            scope.launch {
                uris.take(25 - items.size).forEach { uri ->
                    val isVideo = MediaUtils.isVideoContentUri(context, uri)
                    CampusMediaRepository.uploadPickedMediaUri(
                        context = context,
                        uri = uri,
                        isAdmin = isAdmin
                    ).onSuccess { uploaded ->
                        items.add(
                            HighlightMediaItem(
                                mediaUrl = uploaded,
                                mediaType = if (isVideo) "video" else "image",
                                caption = ""
                            )
                        )
                    }.onFailure {
                        onShowToast(it.localizedMessage ?: "Unable to upload selected media")
                    }
                }
                isUploadingMedia = false
            }
        }
    }

    val quickEmojis = remember { listOf("✨", "🎓", "🏆", "📸", "⚽", "🎨", "💻", "🎵", "📚") }
    val previewCoverUrl = customCoverAvatar ?: items.firstOrNull()?.mediaUrl

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (existingHighlight == null) "New Story Highlight" else "Edit Highlight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Custom Cover Avatar Preview & Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isUploadingCover) {
                                    coverPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("highlight_cover_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isUploadingCover -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                !previewCoverUrl.isNullOrBlank() -> CampusImage(
                                    urlOrDataUri = previewCoverUrl,
                                    contentDescription = "Highlight cover avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                !selectedEmoji.isNullOrBlank() -> Text(selectedEmoji.orEmpty(), fontSize = 26.sp)
                                else -> Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Choose cover avatar",
                                    tint = InstaBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Highlight Cover Avatar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coverPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isUploadingCover,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (customCoverAvatar == null) "Choose Photo" else "Change Photo", fontSize = 12.sp)
                                }
                                if (customCoverAvatar != null) {
                                    TextButton(onClick = { customCoverAvatar = null }) {
                                        Text("Reset", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 40) title = it },
                        label = { Text("Highlight Name") },
                        placeholder = { Text("e.g. Farewell, Cricket, Farewell 2026...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("highlight_title_input")
                    )

                    // Optional Emoji Icon
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Optional Badge Icon (used if no photo cover)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickEmojis.forEach { emoji ->
                                val selected = selectedEmoji == emoji
                                Surface(
                                    shape = CircleShape,
                                    color = if (selected) InstaBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) InstaBlue else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            selectedEmoji = if (selected) null else emoji
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Add Photos/Videos from Gallery or Own Active Stories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Highlight Moments (${items.size}/25)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = {
                                    mediaPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                enabled = !isUploadingMedia && items.size < 25,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("highlight_add_media_button")
                            ) {
                                if (isUploadingMedia) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uploading...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Photos/Videos", fontSize = 12.sp)
                                }
                            }
                        }

                        if (items.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(items) { idx, item ->
                                    Box(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        CampusImage(
                                            urlOrDataUri = item.mediaUrl,
                                            contentDescription = "Highlight slide ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (item.mediaType == "video" || MediaUtils.isVideoMediaUri(item.mediaUrl)) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .clickable { items.removeAt(idx) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (ownActiveStories.isNotEmpty()) {
                            Text(
                                text = "Or tap from your active 24h stories:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(ownActiveStories) { _, st ->
                                    val alreadyAdded = items.any { it.mediaUrl == st.mediaUrl }
                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (alreadyAdded) 2.dp else 0.75.dp,
                                                color = if (alreadyAdded) InstaBlue else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(enabled = !alreadyAdded && items.size < 25) {
                                                items.add(
                                                    HighlightMediaItem(
                                                        mediaUrl = st.mediaUrl,
                                                        mediaType = st.mediaType,
                                                        caption = st.caption
                                                    )
                                                )
                                            }
                                    ) {
                                        CampusImage(
                                            urlOrDataUri = st.mediaUrl,
                                            contentDescription = st.caption.ifBlank { "Story" },
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                title.trim(),
                                customCoverAvatar,
                                selectedEmoji,
                                items.toList()
                            )
                        },
                        enabled = title.trim().isNotBlank() && items.isNotEmpty() && !isUploadingCover && !isUploadingMedia,
                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("highlight_save_button")
                    ) {
                        Text(if (existingHighlight == null) "Create Highlight" else "Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightViewerDialog(
    highlight: StoryHighlight,
    owner: CampusUser?,
    isOwnHighlight: Boolean,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val slides = highlight.items
    if (slides.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var slideIndex by remember(highlight.id) { mutableIntStateOf(0) }
    val safeIndex = slideIndex.coerceIn(0, slides.lastIndex)
    val currentSlide = slides[safeIndex]

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember(highlight.id, safeIndex) { mutableFloatStateOf(0f) }
    var confirmDelete by remember { mutableStateOf(false) }

    val advanceNext: () -> Unit = {
        if (safeIndex < slides.lastIndex) {
            slideIndex = safeIndex + 1
        } else {
            onDismiss()
        }
    }

    val goPrev: () -> Unit = {
        if (safeIndex > 0) {
            slideIndex = safeIndex - 1
        } else {
            progress = 0f
        }
    }

    LaunchedEffect(highlight.id, safeIndex, isPaused, confirmDelete) {
        val durationMs = if (currentSlide.mediaType == "video" || MediaUtils.isVideoMediaUri(currentSlide.mediaUrl)) {
            15_000L
        } else {
            6_000L
        }
        val tickMs = 50L
        while (progress < 1f) {
            if (!isPaused && !confirmDelete) {
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(highlight.id, safeIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                if (offset.x < size.width * 0.34f) goPrev() else advanceNext()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentSlide.mediaType == "video" || MediaUtils.isVideoMediaUri(currentSlide.mediaUrl)) {
                    CampusVideoPlayer(
                        videoUri = currentSlide.mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true
                    )
                } else {
                    CampusImage(
                        urlOrDataUri = currentSlide.mediaUrl,
                        contentDescription = highlight.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent)))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    slides.forEachIndexed { idx, _ ->
                        val seg = when {
                            idx < safeIndex -> 1f
                            idx == safeIndex -> progress
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
                                    .fillMaxWidth(seg.coerceIn(0f, 1f))
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val coverUrl = highlight.coverAvatar ?: slides.firstOrNull()?.mediaUrl
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!coverUrl.isNullOrBlank()) {
                                CampusImage(
                                    urlOrDataUri = coverUrl,
                                    contentDescription = highlight.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (!highlight.coverEmoji.isNullOrBlank()) {
                                Text(highlight.coverEmoji, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlight.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (owner != null) {
                                Text(
                                    text = if (owner.isBlackSociety) "Member - ${owner.blackSocietyNumber ?: 1}" else owner.name,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwnHighlight) {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onEdit()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit highlight", tint = Color.White)
                            }
                        }
                        if (isOwnHighlight || isAdmin) {
                            IconButton(
                                onClick = { confirmDelete = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete highlight", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close highlight", tint = Color.White)
                        }
                    }
                }
            }

            if (currentSlide.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = currentSlide.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Delete Highlight?") },
            text = { Text("Remove \"${highlight.title}\" from profile highlights?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                        onDismiss()
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
}
