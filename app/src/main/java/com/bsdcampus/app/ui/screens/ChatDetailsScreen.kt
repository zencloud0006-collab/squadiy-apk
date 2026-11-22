package com.bsdcampus.app.ui.screens

import android.media.MediaPlayer
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.ChatBackgroundPreset
import com.bsdcampus.app.model.ChatPreference
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.CircularAvatarPreviewDialog
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.components.ZoomableImageLightbox
import com.bsdcampus.app.ui.components.formatRelativeTime
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChatSharedTab(val title: String) {
    MEDIA("Media"),
    LINKS("Links"),
    AUDIO("Audio"),
    STORIES("Stories")
}

private data class ChatSharedLinkItem(
    val url: String,
    val message: DirectMessage,
    val senderName: String
)

private fun extractUrlsFromText(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val matcher = Patterns.WEB_URL.matcher(text)
    val urls = mutableListOf<String>()
    while (matcher.find()) {
        val raw = matcher.group()?.trim() ?: continue
        val normalized = if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            raw
        } else {
            "https://$raw"
        }
        if (normalized !in urls) urls.add(normalized)
    }
    return urls
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatDetailsScreen(
    currentUser: CampusUser,
    peer: CampusUser,
    threadMessages: List<DirectMessage>,
    chatPreference: ChatPreference?,
    defaultBackgroundPreset: String = ChatBackgroundPreset.DEFAULT.key,
    defaultBackgroundImage: String? = null,
    isBlocked: Boolean,
    onBack: () -> Unit,
    onOpenPeerProfile: () -> Unit,
    onSetNickname: (String?) -> Unit,
    onSetBackground: (presetKey: String, imageUri: String?) -> Unit,
    onToggleBlock: () -> Unit,
    onSubmitChatReport: (reason: String, details: String, threadMessages: List<DirectMessage>) -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val realPeerName = if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name
    val activeNickname = chatPreference?.nickname?.takeIf { it.isNotBlank() }
    val displayPeerName = if (peer.isBlackSociety) realPeerName else (activeNickname ?: realPeerName)
    val activePreset = ChatBackgroundPreset.fromKey(chatPreference?.backgroundPreset)
    val hasCustomImageBg = !chatPreference?.backgroundImage.isNullOrBlank()
    val defaultPreset = ChatBackgroundPreset.fromKey(defaultBackgroundPreset)
    val hasDefaultCustomImageBg = !defaultBackgroundImage.isNullOrBlank()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAvatarPreview by remember { mutableStateOf(false) }
    var lightboxStartIndex by remember { mutableStateOf<Int?>(null) }

    var playingAudioId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isUploadingBg by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    val mediaMessages = remember(threadMessages) {
        threadMessages
            .filter { !it.mediaData.isNullOrBlank() }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }
    val allMediaUrls = remember(mediaMessages) {
        mediaMessages.mapNotNull { it.mediaData?.takeIf { url -> url.isNotBlank() } }
    }
    val linkItems = remember(threadMessages, currentUser.uid, displayPeerName) {
        threadMessages
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            .flatMap { msg ->
                val sender = if (msg.senderId == currentUser.uid) "You" else displayPeerName
                extractUrlsFromText(msg.content).map { url ->
                    ChatSharedLinkItem(url = url, message = msg, senderName = sender)
                }
            }
    }
    val audioMessages = remember(threadMessages) {
        threadMessages
            .filter { !it.audioData.isNullOrBlank() }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }
    val storyMessages = remember(threadMessages) {
        threadMessages
            .filter { !it.storyId.isNullOrBlank() || !it.storyMediaUrl.isNullOrBlank() }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

    fun toggleAudio(msg: DirectMessage) {
        if (playingAudioId == msg.id) {
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            playingAudioId = null
            return
        }
        val bytes = MediaUtils.decodeDataUriBytes(msg.audioData) ?: return
        try {
            mediaPlayer?.release()
            val tmp = File(context.cacheDir, "details_audio_${msg.id}.m4a")
            tmp.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(tmp.absolutePath)
                prepare()
                setOnCompletionListener {
                    playingAudioId = null
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
            mediaPlayer = mp
            playingAudioId = msg.id
        } catch (_: Exception) {
            playingAudioId = null
            onShowToast("Unable to play audio clip")
        }
    }

    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingBg = true
            scope.launch {
                CampusMediaRepository.uploadPickedMediaUri(
                    context = context,
                    uri = uri,
                    isAdmin = currentUser.isAdminUser
                ).onSuccess { uploadedRef ->
                    onSetBackground(ChatBackgroundPreset.DEFAULT.key, uploadedRef)
                    showBackgroundDialog = false
                }.onFailure { err ->
                    onShowToast(err.localizedMessage ?: "Unable to upload chat background")
                }
                isUploadingBg = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Header
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to chat"
                        )
                    }
                    Text(
                        text = "Chat Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Profile Section
                item(key = "hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showAvatarPreview = true }
                                .testTag("chat_details_avatar")
                        ) {
                            UserAvatar(
                                src = peer.avatar,
                                name = peer.name,
                                role = peer.role,
                                isVerified = peer.isVerified,
                                isBlackSociety = peer.isBlackSociety,
                                blackSocietyNumber = peer.blackSocietyNumber,
                                isClassRepresentative = peer.isClassRepresentative,
                                size = AvatarSize.XL
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayPeerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (peer.isVerified && !peer.isBlackSociety) {
                                Spacer(modifier = Modifier.width(6.dp))
                                VerifiedBadgeIcon(size = 16.dp)
                            }
                        }
                        if (!peer.isBlackSociety && activeNickname != null) {
                            Text(
                                text = peer.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = buildString {
                                append(peer.grade ?: peer.role.replaceFirstChar { it.uppercase() })
                                if (isBlocked) append(" • Blocked")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBlocked) InstaLikeRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ChatDetailQuickAction(
                                icon = Icons.Outlined.Person,
                                label = "Profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                testTag = "chat_details_profile_action",
                                onClick = onOpenPeerProfile
                            )
                            ChatDetailQuickAction(
                                icon = Icons.Filled.Edit,
                                label = "Nickname",
                                tint = InstaBlue,
                                containerColor = InstaBlue.copy(alpha = 0.12f),
                                testTag = "chat_details_nickname_action",
                                onClick = { showNicknameDialog = true }
                            )
                            ChatDetailQuickAction(
                                icon = Icons.Outlined.Wallpaper,
                                label = "Theme",
                                tint = InstaBlue,
                                containerColor = InstaBlue.copy(alpha = 0.12f),
                                testTag = "chat_details_background_action",
                                onClick = { showBackgroundDialog = true }
                            )
                            ChatDetailQuickAction(
                                icon = Icons.Filled.Block,
                                label = if (isBlocked) "Unblock" else "Block",
                                tint = if (isBlocked) MaterialTheme.colorScheme.onSurface else InstaLikeRed,
                                containerColor = if (isBlocked) MaterialTheme.colorScheme.surfaceVariant else InstaLikeRed.copy(alpha = 0.12f),
                                testTag = "chat_details_block_action",
                                onClick = { showBlockConfirmDialog = true }
                            )
                            ChatDetailQuickAction(
                                icon = Icons.Outlined.Flag,
                                label = "Report",
                                tint = InstaLikeRed,
                                containerColor = InstaLikeRed.copy(alpha = 0.12f),
                                testTag = "chat_details_report_action",
                                onClick = { showReportDialog = true }
                            )
                        }
                    }
                }

                // Customization Cards (Nickname + Custom Background)
                item(key = "customization_card") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNicknameDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("chat_details_nickname_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = InstaBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nickname",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = activeNickname ?: "Set a custom nickname for this chat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (activeNickname != null) "Edit" else "Set",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = InstaBlue
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBackgroundDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("chat_details_background_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = InstaBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chat Background",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = when {
                                            hasCustomImageBg -> "Custom wallpaper photo (shared)"
                                            activePreset != ChatBackgroundPreset.DEFAULT -> "${activePreset.label} theme (shared)"
                                            hasDefaultCustomImageBg -> "Settings default photo"
                                            defaultPreset != ChatBackgroundPreset.DEFAULT -> "Settings default (${defaultPreset.label})"
                                            else -> "Default theme"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (hasCustomImageBg) {
                                    CampusImage(
                                        urlOrDataUri = chatPreference?.backgroundImage.orEmpty(),
                                        contentDescription = "Active background photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                } else if (activePreset != ChatBackgroundPreset.DEFAULT) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Brush.verticalGradient(activePreset.colorsArgb.map { Color(it) }))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = "Choose",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = InstaBlue
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Shared Content Header + Tabs
                item(key = "shared_tabs") {
                    Text(
                        text = "Shared in this chat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        ChatSharedTab.entries.forEachIndexed { idx, tab ->
                            val count = when (tab) {
                                ChatSharedTab.MEDIA -> mediaMessages.size
                                ChatSharedTab.LINKS -> linkItems.size
                                ChatSharedTab.AUDIO -> audioMessages.size
                                ChatSharedTab.STORIES -> storyMessages.size
                            }
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = {
                                    Text(
                                        text = if (count > 0) "${tab.title} ($count)" else tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("chat_details_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (ChatSharedTab.entries[selectedTab]) {
                    ChatSharedTab.MEDIA -> {
                        if (mediaMessages.isEmpty()) {
                            item(key = "empty_media") {
                                SharedEmptyPlaceholder(
                                    icon = Icons.Outlined.Collections,
                                    title = "No shared photos or videos",
                                    subtitle = "Photos and videos shared in this chat will appear here."
                                )
                            }
                        } else {
                            val rows = mediaMessages.chunked(3)
                            items(rows.size, key = { "media_row_$it" }) { rowIdx ->
                                val rowItems = rows[rowIdx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowItems.forEachIndexed { colIdx, msg ->
                                        val globalIndex = rowIdx * 3 + colIdx
                                        val url = msg.mediaData.orEmpty()
                                        val isVideo = MediaUtils.isVideoMediaUri(url)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { lightboxStartIndex = globalIndex }
                                                .testTag("chat_details_media_item_${msg.id}")
                                        ) {
                                            CampusImage(
                                                urlOrDataUri = url,
                                                contentDescription = if (isVideo) "Shared video" else "Shared photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (isVideo) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Black.copy(alpha = 0.65f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PlayArrow,
                                                        contentDescription = "Video",
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .padding(4.dp)
                                                            .size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    ChatSharedTab.LINKS -> {
                        if (linkItems.isEmpty()) {
                            item(key = "empty_links") {
                                SharedEmptyPlaceholder(
                                    icon = Icons.Outlined.Link,
                                    title = "No shared links",
                                    subtitle = "Web links sent in messages will show up here for quick access."
                                )
                            }
                        } else {
                            items(linkItems.size, key = { "link_${linkItems[it].message.id}_$it" }) { idx ->
                                val item = linkItems[idx]
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            runCatching { uriHandler.openUri(item.url) }
                                                .onFailure { onShowToast("Unable to open link") }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(InstaBlue.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Link,
                                                contentDescription = null,
                                                tint = InstaBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.url,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = InstaBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.senderName} • ${formatRelativeTime(item.message.createdAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                            contentDescription = "Open link",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ChatSharedTab.AUDIO -> {
                        if (audioMessages.isEmpty()) {
                            item(key = "empty_audio") {
                                SharedEmptyPlaceholder(
                                    icon = Icons.Outlined.GraphicEq,
                                    title = "No voice notes yet",
                                    subtitle = "Voice notes recorded in this conversation appear here."
                                )
                            }
                        } else {
                            items(audioMessages, key = { "audio_${it.id}" }) { msg ->
                                val isMine = msg.senderId == currentUser.uid
                                val isPlaying = playingAudioId == msg.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { toggleAudio(msg) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isPlaying) InstaLikeRed.copy(alpha = 0.14f) else InstaBlue.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                contentDescription = if (isPlaying) "Stop" else "Play",
                                                tint = if (isPlaying) InstaLikeRed else InstaBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${if (isMine) "You" else displayPeerName} • Voice note",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${msg.audioDurationSec ?: 1}s • ${formatRelativeTime(msg.createdAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isPlaying) {
                                            Text(
                                                text = "Playing",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = InstaLikeRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ChatSharedTab.STORIES -> {
                        if (storyMessages.isEmpty()) {
                            item(key = "empty_stories") {
                                SharedEmptyPlaceholder(
                                    icon = Icons.Outlined.AutoStories,
                                    title = "No story replies",
                                    subtitle = "Story replies shared between you and $displayPeerName appear here."
                                )
                            }
                        } else {
                            items(storyMessages, key = { "story_${it.id}" }) { msg ->
                                val isMine = msg.senderId == currentUser.uid
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!msg.storyMediaUrl.isNullOrBlank() &&
                                                msg.storyMediaType != "post_card" &&
                                                !msg.storyMediaUrl.startsWith("shared_text_post://")
                                            ) {
                                                CampusImage(
                                                    urlOrDataUri = msg.storyMediaUrl,
                                                    contentDescription = "Story thumbnail",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.AutoStories,
                                                    contentDescription = null,
                                                    tint = InstaBlue
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isMine) "You replied to their story" else "$displayPeerName replied to your story",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = InstaBlue
                                            )
                                            if (msg.content.isNotBlank()) {
                                                Text(
                                                    text = msg.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = formatRelativeTime(msg.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Nickname Dialog
    if (showNicknameDialog) {
        var nickInput by remember(activeNickname) { mutableStateOf(activeNickname.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Set Nickname", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Visible to both you and $realPeerName in this chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nickInput,
                        onValueChange = { if (it.length <= 40) nickInput = it },
                        label = { Text("Nickname") },
                        placeholder = { Text(realPeerName) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_nickname_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetNickname(nickInput.trim().takeIf { it.isNotBlank() })
                        showNicknameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                    modifier = Modifier.testTag("chat_nickname_save")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (activeNickname != null) {
                        TextButton(
                            onClick = {
                                onSetNickname(null)
                                showNicknameDialog = false
                            }
                        ) {
                            Text("Reset", color = InstaLikeRed)
                        }
                    }
                    TextButton(onClick = { showNicknameDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Background Picker Dialog
    if (showBackgroundDialog) {
        Dialog(onDismissRequest = { if (!isUploadingBg) showBackgroundDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chat Background",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(
                            onClick = { if (!isUploadingBg) showBackgroundDialog = false }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        text = "Choose a theme gradient or pick a photo (synced for both of you in this chat).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChatBackgroundPreset.entries.forEach { preset ->
                            val isSelected = !hasCustomImageBg && activePreset == preset
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .width(66.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isUploadingBg) {
                                        onSetBackground(preset.key, null)
                                        showBackgroundDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("chat_bg_preset_${preset.key}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (preset == ChatBackgroundPreset.DEFAULT) {
                                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                            } else {
                                                Modifier.background(
                                                    Brush.verticalGradient(preset.colorsArgb.map { Color(it) })
                                                )
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) InstaBlue else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (preset.isDarkPreset) Color.White else InstaBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    Button(
                        onClick = {
                            bgPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploadingBg,
                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("chat_bg_pick_photo")
                    ) {
                        if (isUploadingBg) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading Photo...")
                        } else {
                            Icon(Icons.Outlined.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Photo From Gallery")
                        }
                    }

                    if (hasCustomImageBg || activePreset != ChatBackgroundPreset.DEFAULT) {
                        OutlinedButton(
                            onClick = {
                                onSetBackground(ChatBackgroundPreset.DEFAULT.key, null)
                                showBackgroundDialog = false
                            },
                            enabled = !isUploadingBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("chat_bg_reset")
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset to Default Background")
                        }
                    }
                }
            }
        }
    }

    // Block / Unblock Confirmation Dialog
    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = {
                Text(
                    text = if (isBlocked) "Unblock $realPeerName?" else "Block $realPeerName?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBlocked) {
                        "You will be able to message $realPeerName and see their posts, reels, and stories again."
                    } else {
                        "Blocking hides their posts, reels, and stories from your feed and prevents sending messages to each other until you unblock."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleBlock()
                        showBlockConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) InstaBlue else InstaLikeRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("chat_details_block_confirm")
                ) {
                    Text(if (isBlocked) "Unblock" else "Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Chat Dialog (forwards up to last 5 messages to Admin)
    if (showReportDialog) {
        val reasons = listOf(
            "Harassment or Bullying",
            "Spam or Scam",
            "Inappropriate Messages",
            "Impersonation",
            "Other"
        )
        var selectedReason by remember { mutableStateOf(reasons.first()) }
        var extraDetails by remember { mutableStateOf("") }
        val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
        val recentFivePreview = remember(threadMessages, currentUser.uid, realPeerName) {
            threadMessages
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                .takeLast(5)
                .map { msg ->
                    val sender = if (msg.senderId == currentUser.uid) "You" else realPeerName
                    val timeStr = msg.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Recent"
                    val summary = when {
                        msg.content.isNotBlank() -> msg.content.trim()
                        !msg.audioData.isNullOrBlank() -> "[Voice note ${msg.audioDurationSec ?: 0}s]"
                        MediaUtils.isVideoMediaUri(msg.mediaData) -> "[Video]"
                        !msg.mediaData.isNullOrBlank() -> "[Photo]"
                        !msg.storyId.isNullOrBlank() -> "[Story reply]"
                        else -> "[Message]"
                    }
                    "[$timeStr] $sender: $summary"
                }
        }

        Dialog(onDismissRequest = { showReportDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Report Chat with $realPeerName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (recentFivePreview.isNotEmpty()) {
                            "To help Campus Admin review this report, the latest ${recentFivePreview.size} message(s) from this chat will be sent to Admin."
                        } else {
                            "Submit a report for this conversation to Campus Admin for review."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (recentFivePreview.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Last ${recentFivePreview.size} message(s) included:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = InstaBlue
                                )
                                recentFivePreview.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reasons.forEach { r ->
                            FilterChip(
                                selected = selectedReason == r,
                                onClick = { selectedReason = r },
                                label = { Text(r, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = extraDetails,
                        onValueChange = { if (it.length <= 400) extraDetails = it },
                        label = { Text("Additional details (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_report_details_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSubmitChatReport(selectedReason, extraDetails, threadMessages)
                                showReportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = InstaLikeRed, contentColor = Color.White),
                            modifier = Modifier.testTag("chat_report_submit_button")
                        ) {
                            Text("Send Report")
                        }
                    }
                }
            }
        }
    }

    if (showAvatarPreview) {
        CircularAvatarPreviewDialog(
            avatarSrc = if (peer.isBlackSociety) null else peer.avatar,
            displayName = displayPeerName,
            subtitle = if (!peer.isBlackSociety && activeNickname != null) realPeerName else null,
            isVerified = peer.isVerified,
            isBlackSociety = peer.isBlackSociety,
            blackSocietyNumber = peer.blackSocietyNumber,
            onDismiss = { showAvatarPreview = false }
        )
    }

    lightboxStartIndex?.let { startIdx ->
        ZoomableImageLightbox(
            mediaUrls = allMediaUrls,
            initialPage = startIdx,
            authorName = displayPeerName,
            onDismiss = { lightboxStartIndex = null }
        )
    }
}

@Composable
private fun ChatDetailQuickAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SharedEmptyPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(InstaBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InstaBlue,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
