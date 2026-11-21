package com.bsdcampus.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusNote
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.ConversationSummary
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.model.NoteBubbleColor
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.CampusVideoPlayer
import com.bsdcampus.app.ui.components.NoteAvatarItem
import com.bsdcampus.app.ui.components.NoteComposerDialog
import com.bsdcampus.app.ui.components.NoteViewerDialog
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.components.ZoomableImageLightbox
import com.bsdcampus.app.ui.components.formatRelativeTime
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaIndigo
import com.bsdcampus.app.ui.theme.InstaLikeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    currentUser: CampusUser,
    allUsersState: UiState<List<CampusUser>>,
    conversations: List<ConversationSummary>,
    messages: List<DirectMessage>,
    notes: List<CampusNote> = emptyList(),
    typingMap: Map<String, Boolean>,
    activeChatUserId: String?,
    onSelectChatUser: (String) -> Unit,
    onClearChatUser: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onSendMessage: (recipientId: String, content: String, mediaData: String?, audioData: String?, durationSec: Long?, replyToMessage: DirectMessage?, replyToSenderName: String?) -> Unit,
    onEditMessage: (messageId: String, newContent: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onToggleReaction: (DirectMessage, String) -> Unit,
    onMarkThreadRead: (String) -> Unit,
    onSetTyping: (recipientId: String, isTyping: Boolean) -> Unit,
    onSaveNote: (text: String, bubbleColor: NoteBubbleColor, musicTrack: StoryMusicTrack?, musicStartSec: Int, onSuccess: () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onDeleteNote: (onDone: () -> Unit) -> Unit = { _ -> },
    onShowToast: (String) -> Unit
) {
    val allUsers = (allUsersState as? UiState.Success)?.data.orEmpty()
    val activePeer = remember(allUsers, activeChatUserId) {
        allUsers.firstOrNull { it.uid == activeChatUserId }
    }
    var showNewChatPicker by remember { mutableStateOf(false) }
    var showNoteComposer by remember { mutableStateOf(false) }
    var viewingNote by remember { mutableStateOf<CampusNote?>(null) }

    val ownNote = remember(notes, currentUser.uid) {
        notes.firstOrNull { it.authorId == currentUser.uid && !it.isExpired() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.38f).fillMaxHeight()) {
                    ConversationListPane(
                        currentUser = currentUser,
                        allUsers = allUsers,
                        conversations = conversations,
                        notes = notes,
                        selectedPeerId = activeChatUserId,
                        onSelectPeer = onSelectChatUser,
                        onOpenNewChat = { showNewChatPicker = true },
                        onOpenOwnNote = {
                            if (ownNote != null) viewingNote = ownNote else showNoteComposer = true
                        },
                        onViewPeerNote = { viewingNote = it }
                    )
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline)
                )
                Box(modifier = Modifier.weight(0.62f).fillMaxHeight()) {
                    if (activePeer != null) {
                        ChatThreadPane(
                            currentUser = currentUser,
                            peer = activePeer,
                            threadMessages = messages.filter {
                                (it.senderId == currentUser.uid && it.recipientId == activePeer.uid) ||
                                    (it.senderId == activePeer.uid && it.recipientId == currentUser.uid)
                            },
                            isPeerTyping = typingMap[activePeer.uid] == true,
                            showBackButton = false,
                            onBack = onClearChatUser,
                            onOpenPeerProfile = { onOpenProfile(activePeer.uid) },
                            onSendMessage = { text, media, audio, dur, replyMsg, replyName ->
                                onSendMessage(activePeer.uid, text, media, audio, dur, replyMsg, replyName)
                            },
                            onEditMessage = onEditMessage,
                            onDeleteMessage = onDeleteMessage,
                            onToggleReaction = onToggleReaction,
                            onMarkRead = { onMarkThreadRead(activePeer.uid) },
                            onSetTyping = { typing -> onSetTyping(activePeer.uid, typing) },
                            onShowToast = onShowToast
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(InstaBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(30.dp), tint = InstaBlue)
                                }
                                Text("Your Campus Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Select someone you follow to start chatting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            if (activePeer != null) {
                BackHandler { onClearChatUser() }
                ChatThreadPane(
                    currentUser = currentUser,
                    peer = activePeer,
                    threadMessages = messages.filter {
                        (it.senderId == currentUser.uid && it.recipientId == activePeer.uid) ||
                            (it.senderId == activePeer.uid && it.recipientId == currentUser.uid)
                    },
                    isPeerTyping = typingMap[activePeer.uid] == true,
                    showBackButton = true,
                    onBack = onClearChatUser,
                    onOpenPeerProfile = { onOpenProfile(activePeer.uid) },
                    onSendMessage = { text, media, audio, dur, replyMsg, replyName ->
                        onSendMessage(activePeer.uid, text, media, audio, dur, replyMsg, replyName)
                    },
                    onEditMessage = onEditMessage,
                    onDeleteMessage = onDeleteMessage,
                    onToggleReaction = onToggleReaction,
                    onMarkRead = { onMarkThreadRead(activePeer.uid) },
                    onSetTyping = { typing -> onSetTyping(activePeer.uid, typing) },
                    onShowToast = onShowToast
                )
            } else {
                ConversationListPane(
                    currentUser = currentUser,
                    allUsers = allUsers,
                    conversations = conversations,
                    notes = notes,
                    selectedPeerId = null,
                    onSelectPeer = onSelectChatUser,
                    onOpenNewChat = { showNewChatPicker = true },
                    onOpenOwnNote = {
                        if (ownNote != null) viewingNote = ownNote else showNoteComposer = true
                    },
                    onViewPeerNote = { viewingNote = it }
                )
            }
        }
    }

    if (showNewChatPicker) {
        var search by remember { mutableStateOf("") }
        val candidates = remember(allUsers, search, currentUser.uid, currentUser.following) {
            allUsers.filter {
                it.uid != currentUser.uid &&
                    currentUser.following.contains(it.uid) &&
                    it.isApproved &&
                    !it.isSuspended &&
                    (search.isBlank() || it.name.contains(search, ignoreCase = true) || (it.grade?.contains(search, ignoreCase = true) == true))
            }
        }
        Dialog(onDismissRequest = { showNewChatPicker = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("New Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                "People you follow",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showNewChatPicker = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search followed people...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (candidates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentUser.following.isEmpty()) {
                                    "Follow people on Squadify to message them here."
                                } else {
                                    "No followed people match \"$search\"."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(candidates, key = { it.uid }) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showNewChatPicker = false
                                            onSelectChatUser(user.uid)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        src = user.avatar,
                                        name = user.name,
                                        role = user.role,
                                        isVerified = user.isVerified,
                                        isBlackSociety = user.isBlackSociety,
                                        blackSocietyNumber = user.blackSocietyNumber,
                                        isClassRepresentative = user.isClassRepresentative,
                                        size = AvatarSize.MD
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (user.isBlackSociety) "Member - ${user.blackSocietyNumber ?: 1}" else user.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (user.isVerified && !user.isBlackSociety) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VerifiedBadgeIcon(size = 13.dp)
                                            }
                                        }
                                        Text(
                                            text = formatLastSeenStatus(user, isTyping = false),
                                            style = MaterialTheme.typography.bodySmall,
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

    if (showNoteComposer) {
        NoteComposerDialog(
            currentUser = currentUser,
            existingNote = ownNote,
            onSave = onSaveNote,
            onDelete = onDeleteNote,
            onDismiss = { showNoteComposer = false }
        )
    }

    viewingNote?.let { note ->
        val noteAuthor = remember(allUsers, note.authorId, currentUser) {
            if (note.authorId == currentUser.uid) currentUser
            else allUsers.firstOrNull { it.uid == note.authorId }
        }
        NoteViewerDialog(
            note = note,
            author = noteAuthor,
            isOwnNote = note.authorId == currentUser.uid,
            onEditOwnNote = {
                viewingNote = null
                showNoteComposer = true
            },
            onDeleteOwnNote = {
                viewingNote = null
                onDeleteNote {}
            },
            onQuickReply = { replyText ->
                val noteSnippet = note.text.ifBlank { note.musicTitle ?: "your note" }.take(40)
                onSendMessage(
                    note.authorId,
                    "Replied to your note \"$noteSnippet\": $replyText",
                    null,
                    null,
                    null,
                    null,
                    null
                )
                viewingNote = null
                onSelectChatUser(note.authorId)
            },
            onOpenChat = { onSelectChatUser(note.authorId) },
            onOpenProfile = { onOpenProfile(note.authorId) },
            onDismiss = { viewingNote = null }
        )
    }
}

@Composable
private fun ConversationListPane(
    currentUser: CampusUser,
    allUsers: List<CampusUser>,
    conversations: List<ConversationSummary>,
    notes: List<CampusNote>,
    selectedPeerId: String?,
    onSelectPeer: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    onOpenOwnNote: () -> Unit,
    onViewPeerNote: (CampusNote) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filterUnreadOnly by remember { mutableStateOf(false) }

    val visibleConversations = remember(conversations) {
        conversations.filter { !it.otherUser.isSuspended }
    }
    val unreadThreadsCount = remember(visibleConversations) {
        visibleConversations.count { it.unreadCount > 0 }
    }
    val filtered = remember(visibleConversations, search, filterUnreadOnly) {
        visibleConversations.filter { convo ->
            val matchesSearch = search.isBlank() || convo.otherUser.name.contains(search, ignoreCase = true)
            val matchesUnread = !filterUnreadOnly || convo.unreadCount > 0
            matchesSearch && matchesUnread
        }
    }
    val notesByAuthor = remember(notes) {
        notes.filter { !it.isExpired() }.associateBy { it.authorId }
    }
    val activeQuickUsers = remember(allUsers, currentUser.uid, currentUser.following, notesByAuthor) {
        allUsers
            .filter {
                it.uid != currentUser.uid &&
                    currentUser.following.contains(it.uid) &&
                    it.isApproved &&
                    !it.isSuspended
            }
            .sortedWith(
                compareByDescending<CampusUser> { notesByAuthor.containsKey(it.uid) }
                    .thenByDescending { it.isVerified }
                    .thenByDescending { it.lastSeenAt?.toDate()?.time ?: 0L }
            )
            .take(20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Instagram DM Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (currentUser.isBlackSociety) "Member - ${currentUser.blackSocietyNumber ?: 1}" else currentUser.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentUser.isVerified && !currentUser.isBlackSociety) {
                    Spacer(modifier = Modifier.width(5.dp))
                    VerifiedBadgeIcon(size = 16.dp)
                }
            }
            IconButton(
                onClick = onOpenNewChat,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("chat_new_button")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Sleek Instagram-style rounded search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (search.isEmpty()) {
                        Text(
                            text = "Search chats...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(InstaBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (search.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { search = "" }
                    )
                }
            }
        }

        // Instagram Chat Notes & Followed Quick-Chat Strip (No green dots)
        if (search.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "own_note") {
                    NoteAvatarItem(
                        user = currentUser,
                        note = notesByAuthor[currentUser.uid],
                        isOwn = true,
                        onClick = onOpenOwnNote
                    )
                }
                items(activeQuickUsers, key = { it.uid }) { peer ->
                    val peerNote = notesByAuthor[peer.uid]
                    NoteAvatarItem(
                        user = peer,
                        note = peerNote,
                        isOwn = false,
                        onClick = {
                            if (peerNote != null) onViewPeerNote(peerNote)
                            else onSelectPeer(peer.uid)
                        }
                    )
                }
            }
        }

        // Messages Heading + Inbox Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!filterUnreadOnly) InstaBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { filterUnreadOnly = false }
                        .testTag("chat_filter_all")
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!filterUnreadOnly) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (filterUnreadOnly) InstaBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { filterUnreadOnly = true }
                        .testTag("chat_filter_unread")
                ) {
                    Text(
                        text = if (unreadThreadsCount > 0) "Unread ($unreadThreadsCount)" else "Unread",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (filterUnreadOnly) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(InstaBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(30.dp), tint = InstaBlue)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (filterUnreadOnly) "All caught up!" else "No messages yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        filterUnreadOnly -> "You have no unread messages."
                        currentUser.following.isEmpty() -> "Tap the compose icon to start a conversation or follow people on Explore."
                        else -> "Tap someone above or the compose icon to start chatting."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 104.dp)
            ) {
                items(filtered, key = { it.otherUser.uid }) { convo ->
                    val isSelected = convo.otherUser.uid == selectedPeerId
                    val hasUnread = convo.unreadCount > 0
                    val isLastMine = convo.lastMessage.senderId == currentUser.uid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .clickable { onSelectPeer(convo.otherUser.uid) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            src = convo.otherUser.avatar,
                            name = convo.otherUser.name,
                            role = convo.otherUser.role,
                            isVerified = convo.otherUser.isVerified,
                            isBlackSociety = convo.otherUser.isBlackSociety,
                            blackSocietyNumber = convo.otherUser.blackSocietyNumber,
                            isClassRepresentative = convo.otherUser.isClassRepresentative,
                            size = AvatarSize.LG
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (convo.otherUser.isBlackSociety) "Member - ${convo.otherUser.blackSocietyNumber ?: 1}" else convo.otherUser.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (hasUnread) FontWeight.Black else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (convo.otherUser.isVerified && !convo.otherUser.isBlackSociety) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadgeIcon(size = 14.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val rawPreview = when {
                                !convo.lastMessage.storyId.isNullOrBlank() -> "Replied to story: ${convo.lastMessage.content}"
                                !convo.lastMessage.audioData.isNullOrBlank() -> "🎤 Voice note"
                                MediaUtils.isVideoMediaUri(convo.lastMessage.mediaData) -> "🎥 Video"
                                !convo.lastMessage.mediaData.isNullOrBlank() && convo.lastMessage.content.isBlank() -> "📷 Photo"
                                else -> convo.lastMessage.content
                            }
                            val prefix = if (isLastMine) "You: " else ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLastMine) {
                                    Icon(
                                        imageVector = if (convo.lastMessage.read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = if (convo.lastMessage.read) "Seen" else "Sent",
                                        tint = if (convo.lastMessage.read) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "$prefix$rawPreview",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = " • ${formatRelativeTime(convo.lastMessage.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = InstaBlue
                            ) {
                                Text(
                                    text = if (convo.unreadCount > 9) "9+" else convo.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadPane(
    currentUser: CampusUser,
    peer: CampusUser,
    threadMessages: List<DirectMessage>,
    isPeerTyping: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenPeerProfile: () -> Unit,
    onSendMessage: (content: String, mediaData: String?, audioData: String?, durationSec: Long?, replyToMessage: DirectMessage?, replyToSenderName: String?) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onToggleReaction: (DirectMessage, String) -> Unit,
    onMarkRead: () -> Unit,
    onSetTyping: (Boolean) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draft by remember { mutableStateOf("") }
    var attachedMedia by remember { mutableStateOf<String?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var activeMessageActionTarget by remember { mutableStateOf<DirectMessage?>(null) }
    var editingMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingSafSaveUrl by remember { mutableStateOf<String?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }

    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val safSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { targetUri ->
        val src = pendingSafSaveUrl
        pendingSafSaveUrl = null
        if (targetUri != null && !src.isNullOrBlank()) {
            scope.launch {
                CampusMediaRepository.writeImageToSafUri(context, targetUri, src)
                    .onSuccess { onShowToast("Image saved") }
                    .onFailure { onShowToast(it.localizedMessage ?: "Unable to save image") }
            }
        }
    }

    fun downloadChatImage(imageUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scope.launch {
                CampusMediaRepository.saveImageToGallery(context, imageUrl)
                    .onSuccess { onShowToast(it) }
                    .onFailure { onShowToast(it.localizedMessage ?: "Unable to save image") }
            }
        } else {
            pendingSafSaveUrl = imageUrl
            safSaveLauncher.launch("Squadify_${System.currentTimeMillis()}.jpg")
        }
    }

    fun senderDisplayNameFor(senderId: String): String {
        return if (senderId == currentUser.uid) "You"
        else if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}"
        else peer.name
    }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.release() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            onSetTyping(false)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0L
            while (isRecording) {
                delay(1000L)
                recordingSeconds += 1L
                if (recordingSeconds >= 45L) {
                    break
                }
            }
        }
    }

    LaunchedEffect(threadMessages.size, isPeerTyping, peer.uid) {
        onMarkRead()
        val total = threadMessages.size + if (isPeerTyping) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    LaunchedEffect(draft) {
        if (draft.isNotBlank()) {
            onSetTyping(true)
            delay(2500L)
            onSetTyping(false)
        } else {
            onSetTyping(false)
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingMedia = true
            scope.launch {
                CampusMediaRepository.uploadPickedMediaUri(
                    context = context,
                    uri = uri,
                    isAdmin = currentUser.isAdminUser
                ).onSuccess { mediaRef ->
                    attachedMedia = mediaRef
                }.onFailure { err ->
                    onShowToast(err.message ?: "Unable to attach media")
                }
                isUploadingMedia = false
            }
        }
    }

    fun startVoiceRecording() {
        try {
            val outFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(32_000)
            r.setAudioSamplingRate(22_050)
            r.setOutputFile(outFile.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordFile = outFile
            isRecording = true
        } catch (_: Exception) {
            isRecording = false
            onShowToast("Microphone unavailable on this device")
        }
    }

    fun stopAndSendVoiceRecording() {
        val duration = recordingSeconds.coerceAtLeast(1L)
        isRecording = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null

        val bytes = recordFile?.takeIf { it.exists() }?.readBytes()
        recordFile?.delete()
        recordFile = null

        if (bytes != null && bytes.isNotEmpty()) {
            val encoded = MediaUtils.encodeAudioBytesToDataUrl(bytes)
            if (encoded != null) {
                val replyTarget = replyingToMessage
                onSendMessage(
                    "",
                    null,
                    encoded,
                    duration,
                    replyTarget,
                    replyTarget?.let { senderDisplayNameFor(it.senderId) }
                )
                replyingToMessage = null
            } else {
                onShowToast("Voice note exceeded size limit; record a shorter clip.")
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording()
        else onShowToast("Microphone permission is needed for voice notes.")
    }

    fun toggleAudioPlayback(msg: DirectMessage) {
        if (playingMessageId == msg.id) {
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            playingMessageId = null
            return
        }
        val bytes = MediaUtils.decodeDataUriBytes(msg.audioData) ?: return
        try {
            mediaPlayer?.release()
            val tmp = File(context.cacheDir, "play_${msg.id}.m4a")
            tmp.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(tmp.absolutePath)
                prepare()
                setOnCompletionListener {
                    playingMessageId = null
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
            mediaPlayer = mp
            playingMessageId = msg.id
        } catch (_: Exception) {
            playingMessageId = null
            onShowToast("Unable to play voice note")
        }
    }

    val containerModifier = if (showBackButton) {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    } else {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    }

    Column(modifier = containerModifier) {
        // Instagram DM Chat Header
        Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to messages")
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenPeerProfile),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        src = peer.avatar,
                        name = peer.name,
                        role = peer.role,
                        isVerified = peer.isVerified,
                        isBlackSociety = peer.isBlackSociety,
                        blackSocietyNumber = peer.blackSocietyNumber,
                        isClassRepresentative = peer.isClassRepresentative,
                        size = AvatarSize.MD
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (peer.isVerified && !peer.isBlackSociety) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadgeIcon(size = 14.dp)
                            }
                        }
                        val statusText = remember(peer.lastSeenAt, isPeerTyping) {
                            formatLastSeenStatus(peer, isTyping = isPeerTyping)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPeerTyping || statusText == "Active now") InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isPeerTyping || statusText == "Active now") FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                IconButton(
                    onClick = onOpenPeerProfile,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_header_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "View profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        // Messages Stream (Instagram-style stacking + avatar only on last message in run)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (threadMessages.isEmpty() && !isPeerTyping) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (peer.isVerified && !peer.isBlackSociety) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadgeIcon(size = 15.dp)
                            }
                        }
                        Text(
                            text = "${peer.grade ?: peer.role.replaceFirstChar { it.uppercase() }} • Squadify",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(threadMessages, key = { _, m -> m.id }) { index, msg ->
                val isMine = msg.senderId == currentUser.uid
                val prevMsg = threadMessages.getOrNull(index - 1)
                val nextMsg = threadMessages.getOrNull(index + 1)

                val hasEmbeddedStory = !msg.storyId.isNullOrBlank() || !msg.storyMediaUrl.isNullOrBlank()
                val prevHasEmbeddedStory = prevMsg != null && (!prevMsg.storyId.isNullOrBlank() || !prevMsg.storyMediaUrl.isNullOrBlank())
                val nextHasEmbeddedStory = nextMsg != null && (!nextMsg.storyId.isNullOrBlank() || !nextMsg.storyMediaUrl.isNullOrBlank())

                fun withinGroupWindow(a: DirectMessage?, b: DirectMessage?): Boolean {
                    val t1 = a?.createdAt?.toDate()?.time ?: return true
                    val t2 = b?.createdAt?.toDate()?.time ?: return true
                    return abs(t2 - t1) <= 5 * 60_000L
                }

                val isStackedWithPrev = prevMsg != null &&
                    prevMsg.senderId == msg.senderId &&
                    !hasEmbeddedStory &&
                    !prevHasEmbeddedStory &&
                    msg.replyToMessageId.isNullOrBlank() &&
                    withinGroupWindow(prevMsg, msg)

                val isStackedWithNext = nextMsg != null &&
                    nextMsg.senderId == msg.senderId &&
                    !hasEmbeddedStory &&
                    !nextHasEmbeddedStory &&
                    nextMsg.replyToMessageId.isNullOrBlank() &&
                    withinGroupWindow(msg, nextMsg)

                val showPeerAvatarOnBubble = !isMine && !isStackedWithNext

                val bubbleShape = if (isMine) {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = if (isStackedWithPrev) 6.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = if (isStackedWithNext) 6.dp else 20.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = if (isStackedWithPrev) 6.dp else 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isStackedWithNext) 6.dp else 20.dp,
                        bottomEnd = 20.dp
                    )
                }

                val reactionMap = msg.parsedReactions()
                val swipeThresholdPx = with(density) { 54.dp.toPx() }
                val maxSwipePx = with(density) { 72.dp.toPx() }
                var rawDragPx by remember(msg.id) { mutableFloatStateOf(0f) }
                var hapticTriggered by remember(msg.id) { mutableStateOf(false) }
                val animatedSwipePx by animateFloatAsState(
                    targetValue = rawDragPx,
                    animationSpec = spring(stiffness = 650f, dampingRatio = 0.78f),
                    label = "chatSwipeReply"
                )

                val isLastMessage = index == threadMessages.lastIndex
                val showDayDivider = remember(msg.id, prevMsg?.id) {
                    !isSameCalendarDay(prevMsg?.createdAt?.toDate(), msg.createdAt?.toDate())
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else if (isStackedWithPrev && !showDayDivider) 2.dp else 10.dp)
                        .animateItem()
                ) {
                    if (showDayDivider && msg.createdAt != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = formatChatDayHeader(msg.createdAt.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                    if (abs(animatedSwipePx) > 8f) {
                        val progress = (abs(animatedSwipePx) / swipeThresholdPx).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                                .padding(horizontal = 8.dp)
                                .alpha(progress)
                                .scale(0.8f + 0.25f * progress)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = InstaBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(animatedSwipePx.roundToInt(), 0) }
                            .pointerInput(msg.id, isMine) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (abs(rawDragPx) >= swipeThresholdPx) {
                                            replyingToMessage = msg
                                        }
                                        rawDragPx = 0f
                                        hapticTriggered = false
                                    },
                                    onDragCancel = {
                                        rawDragPx = 0f
                                        hapticTriggered = false
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        val nextOffset = (rawDragPx + dragAmount).let { candidate ->
                                            if (isMine) candidate.coerceIn(-maxSwipePx, 0f)
                                            else candidate.coerceIn(0f, maxSwipePx)
                                        }
                                        rawDragPx = nextOffset
                                        if (abs(nextOffset) >= swipeThresholdPx && !hapticTriggered) {
                                            hapticTriggered = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        if (hasEmbeddedStory) {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = if (isMine) 0.dp else 34.dp,
                                        bottom = 4.dp
                                    ),
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isMine) "You replied to their story" else "Replied to your story",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isMine) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(118.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.size(width = 88.dp, height = 118.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (msg.storyMediaType == "post_card" || msg.storyMediaUrl?.startsWith("shared_text_post://") == true) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color(0xFF141824), Color(0xFF1E1328))
                                                            )
                                                        )
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = msg.storyCaption?.takeIf { it.isNotBlank() } ?: "Reposted Post",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        maxLines = 4,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else if (!msg.storyMediaUrl.isNullOrBlank()) {
                                                CampusImage(
                                                    urlOrDataUri = msg.storyMediaUrl,
                                                    contentDescription = "Story preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (msg.storyMediaType == "video" || MediaUtils.isVideoMediaUri(msg.storyMediaUrl)) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color.Black.copy(alpha = 0.65f),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Video story",
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .padding(3.dp)
                                                                .size(12.dp)
                                                        )
                                                    }
                                                }
                                                if (!msg.storyCaption.isNullOrBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .fillMaxWidth()
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(
                                                                        Color.Transparent,
                                                                        Color.Black.copy(alpha = 0.78f)
                                                                    )
                                                                )
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = msg.storyCaption,
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            lineHeight = 12.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = msg.storyCaption?.takeIf { it.isNotBlank() } ?: "Story expired",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (isMine) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(118.dp)
                                                .clip(CircleShape)
                                                .background(InstaBlue.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }

                        // Quoted Message Reply Preview Block
                        if (!msg.replyToMessageId.isNullOrBlank()) {
                            val replyLabel = when {
                                msg.replyToSenderId == currentUser.uid && isMine -> "You replied to yourself"
                                isMine -> "You replied to ${msg.replyToSenderName ?: peer.name.substringBefore(" ")}"
                                msg.replyToSenderId == currentUser.uid -> "${peer.name.substringBefore(" ")} replied to you"
                                else -> "Replied to ${msg.replyToSenderName ?: "message"}"
                            }
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = if (isMine) 0.dp else 34.dp,
                                        bottom = 3.dp
                                    )
                                    .clickable {
                                        val targetIdx = threadMessages.indexOfFirst { it.id == msg.replyToMessageId }
                                        if (targetIdx >= 0) {
                                            scope.launch { listState.animateScrollToItem(targetIdx) }
                                        }
                                    },
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = replyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(16.dp)
                                                .clip(CircleShape)
                                                .background(InstaBlue)
                                        )
                                        Text(
                                            text = msg.replyToContent?.takeIf { it.isNotBlank() } ?: "Message",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isMine) {
                                if (showPeerAvatarOnBubble) {
                                    UserAvatar(
                                        src = peer.avatar,
                                        name = peer.name,
                                        role = peer.role,
                                        isVerified = peer.isVerified,
                                        isBlackSociety = peer.isBlackSociety,
                                        blackSocietyNumber = peer.blackSocietyNumber,
                                        isClassRepresentative = peer.isClassRepresentative,
                                        size = AvatarSize.XS
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(34.dp))
                                }
                            }

                            val bubbleBgModifier = if (isMine) {
                                Modifier.background(
                                    brush = Brush.horizontalGradient(listOf(InstaBlue, InstaIndigo)),
                                    shape = bubbleShape
                                )
                            } else {
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = bubbleShape
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .widthIn(max = 285.dp)
                                    .animateContentSize()
                                    .clip(bubbleShape)
                                    .then(bubbleBgModifier)
                                    .pointerInput(msg.id) {
                                        detectTapGestures(
                                            onDoubleTap = { onToggleReaction(msg, "❤️") },
                                            onTap = { activeMessageActionTarget = msg },
                                            onLongPress = { activeMessageActionTarget = msg }
                                        )
                                    }
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!msg.mediaData.isNullOrBlank()) {
                                        if (MediaUtils.isVideoMediaUri(msg.mediaData)) {
                                            CampusVideoPlayer(
                                                videoUri = msg.mediaData,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(210.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(190.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .pointerInput(msg.id, msg.mediaData) {
                                                        detectTapGestures(
                                                            onTap = { zoomedImageUrl = msg.mediaData },
                                                            onDoubleTap = { onToggleReaction(msg, "❤️") },
                                                            onLongPress = { activeMessageActionTarget = msg }
                                                        )
                                                    }
                                            ) {
                                                CampusImage(
                                                    urlOrDataUri = msg.mediaData,
                                                    contentDescription = "Chat photo attachment",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Black.copy(alpha = 0.56f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(30.dp)
                                                        .clickable { downloadChatImage(msg.mediaData) }
                                                        .testTag("chat_image_download_${msg.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Download,
                                                            contentDescription = "Download photo",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!msg.audioData.isNullOrBlank()) {
                                        val isPlaying = playingMessageId == msg.id
                                        val waveHeights = remember { listOf(8, 16, 12, 20, 14, 22, 10, 18, 13, 15, 9) }
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.Black.copy(alpha = 0.16f))
                                                .clickable { toggleAudioPlayback(msg) }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                contentDescription = if (isPlaying) "Stop voice note" else "Play voice note",
                                                tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                waveHeights.forEach { barDp ->
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(barDp.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isMine) Color.White.copy(alpha = if (isPlaying) 0.95f else 0.72f)
                                                                else InstaBlue.copy(alpha = if (isPlaying) 0.95f else 0.65f)
                                                            )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isPlaying) "Playing" else "${msg.audioDurationSec ?: 1}s",
                                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    if (msg.content.isNotBlank()) {
                                        Text(
                                            text = msg.content,
                                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    val showTimestampOrSeen = !isStackedWithNext || msg.edited || reactionMap.isNotEmpty()
                                    if (showTimestampOrSeen) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = buildString {
                                                    append(formatRelativeTime(msg.createdAt))
                                                    if (msg.edited) append(" • edited")
                                                },
                                                color = if (isMine) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                            if (isMine) {
                                                Icon(
                                                    imageVector = if (msg.read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                                    contentDescription = if (msg.read) "Seen" else "Sent",
                                                    tint = if (msg.read) Color.White else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (reactionMap.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(top = 3.dp, start = if (isMine) 0.dp else 34.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                reactionMap.forEach { (emoji, usersList) ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.clickable { onToggleReaction(msg, emoji) }
                                    ) {
                                        Text(
                                            text = "$emoji ${usersList.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                    if (isLastMessage && isMine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (msg.read) {
                                Text(
                                    text = "Seen",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier.size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        src = peer.avatar,
                                        name = peer.name,
                                        role = peer.role,
                                        isVerified = false,
                                        isBlackSociety = peer.isBlackSociety,
                                        blackSocietyNumber = peer.blackSocietyNumber,
                                        isClassRepresentative = peer.isClassRepresentative,
                                        size = AvatarSize.XS,
                                        modifier = Modifier.scale(0.66f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Sent",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (isPeerTyping) {
                item(key = "typing_indicator") {
                    Box(modifier = Modifier.padding(top = 8.dp)) {
                        TypingBubbleIndicator(peer = peer)
                    }
                }
            }
        }

        // Reply Preview Bar in Composer
        replyingToMessage?.let { target ->
            val targetName = if (target.senderId == currentUser.uid) {
                "yourself"
            } else {
                if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name.substringBefore(" ")
            }
            val snippet = target.content.ifBlank {
                when {
                    !target.storyId.isNullOrBlank() -> "Story"
                    !target.audioData.isNullOrBlank() -> "Voice note"
                    MediaUtils.isVideoMediaUri(target.mediaData) -> "Video"
                    !target.mediaData.isNullOrBlank() -> "Photo"
                    else -> "Message"
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("chat_reply_preview_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(InstaBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Replying to $targetName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = InstaBlue
                    )
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { replyingToMessage = null }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                }
            }
        }

        // Attached media preview
        attachedMedia?.let { mediaRef ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampusImage(
                    urlOrDataUri = mediaRef,
                    contentDescription = "Pending attachment",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (MediaUtils.isVideoMediaUri(mediaRef)) "Video ready to send" else "Photo ready to send",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { attachedMedia = null }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
                }
            }
        }

        // Instagram Pill Composer Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    enabled = !isUploadingMedia && !isRecording,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isUploadingMedia) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Attach Photo or Video",
                            tint = InstaBlue
                        )
                    }
                }

                val recTransition = rememberInfiniteTransition(label = "micPulse")
                val micPulseScale by recTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRecording) 1.18f else 1f,
                    animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
                    label = "micScale"
                )
                IconButton(
                    onClick = {
                        if (isRecording) {
                            stopAndSendVoiceRecording()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) startVoiceRecording() else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .scale(if (isRecording) micPulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isRecording) InstaLikeRed.copy(alpha = 0.16f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop and send voice note" else "Record voice note",
                        tint = if (isRecording) InstaLikeRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isRecording) {
                        Text(
                            text = "Recording voice note (${recordingSeconds}s)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InstaLikeRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Message...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(InstaBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                    }
                }

                val canSend = !isRecording && !isUploadingMedia && (draft.isNotBlank() || attachedMedia != null)
                if (canSend) {
                    IconButton(
                        onClick = {
                            val replyTarget = replyingToMessage
                            onSendMessage(
                                draft.trim(),
                                attachedMedia,
                                null,
                                null,
                                replyTarget,
                                replyTarget?.let { senderDisplayNameFor(it.senderId) }
                            )
                            draft = ""
                            attachedMedia = null
                            replyingToMessage = null
                            onSetTyping(false)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (!isRecording && !isUploadingMedia) {
                    IconButton(
                        onClick = {
                            val replyTarget = replyingToMessage
                            onSendMessage(
                                "❤️",
                                null,
                                null,
                                null,
                                replyTarget,
                                replyTarget?.let { senderDisplayNameFor(it.senderId) }
                            )
                            replyingToMessage = null
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .testTag("chat_quick_heart_button")
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Send heart",
                            tint = InstaLikeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Message Reactions / Reply / Save / Edit / Delete Sheet Dialog
    activeMessageActionTarget?.let { msg ->
        val isMine = msg.senderId == currentUser.uid
        val hasSaveableImage = !msg.mediaData.isNullOrBlank() && !MediaUtils.isVideoMediaUri(msg.mediaData)
        Dialog(onDismissRequest = { activeMessageActionTarget = null }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Message Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CampusConstants.REACTION_EMOJIS.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onToggleReaction(msg, emoji)
                                        activeMessageActionTarget = null
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyingToMessage = msg
                                activeMessageActionTarget = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("chat_action_reply"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Reply")
                    }
                    if (hasSaveableImage) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val url = msg.mediaData
                                    activeMessageActionTarget = null
                                    if (!url.isNullOrBlank()) downloadChatImage(url)
                                }
                                .padding(vertical = 8.dp)
                                .testTag("chat_action_save_photo"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Save Photo")
                        }
                    }
                    if (isMine && msg.content.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editDraft = msg.content
                                    editingMessage = msg
                                    activeMessageActionTarget = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Edit Message")
                        }
                    }
                    if (isMine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDeleteMessage(msg.id)
                                    activeMessageActionTarget = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = InstaLikeRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Delete Message", color = InstaLikeRed)
                        }
                    }
                }
            }
        }
    }

    editingMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editDraft.isNotBlank()) {
                            onEditMessage(target.id, editDraft.trim())
                            editingMessage = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("Cancel") }
            }
        )
    }

    zoomedImageUrl?.let { imageUrl ->
        ZoomableImageLightbox(
            mediaUrls = listOf(imageUrl),
            authorName = if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name,
            onDownload = { downloadChatImage(it) },
            onDismiss = { zoomedImageUrl = null }
        )
    }
}

@Composable
private fun TypingBubbleIndicator(peer: CampusUser) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 0), RepeatMode.Reverse),
        label = "d1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), RepeatMode.Reverse),
        label = "d2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), RepeatMode.Reverse),
        label = "d3"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(
            src = peer.avatar,
            name = peer.name,
            role = peer.role,
            isVerified = peer.isVerified,
            isBlackSociety = peer.isBlackSociety,
            blackSocietyNumber = peer.blackSocietyNumber,
            isClassRepresentative = peer.isClassRepresentative,
            size = AvatarSize.XS
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(alpha1, alpha2, alpha3).forEach { a ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = a))
                    )
                }
            }
        }
    }
}

private fun formatLastSeenStatus(user: CampusUser, isTyping: Boolean): String {
    if (isTyping) return "typing..."
    val lastSeenMs = user.lastSeenAt?.toDate()?.time ?: return "Last seen recently"
    val diffSec = ((System.currentTimeMillis() - lastSeenMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 90L -> "Active now"
        diffSec < 3600L -> "Last seen ${(diffSec / 60L).coerceAtLeast(1L)}m ago"
        diffSec < 86_400L -> "Last seen ${diffSec / 3600L}h ago"
        else -> {
            val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            "Last seen ${fmt.format(Date(lastSeenMs))}"
        }
    }
}

private fun isSameCalendarDay(a: Date?, b: Date?): Boolean {
    if (a == null || b == null) return true
    val calA = Calendar.getInstance().apply { time = a }
    val calB = Calendar.getInstance().apply { time = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

private fun formatChatDayHeader(date: Date): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Today"
    }
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Yesterday"
    }
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
}
