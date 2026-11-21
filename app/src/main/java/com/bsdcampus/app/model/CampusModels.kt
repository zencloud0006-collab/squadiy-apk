package com.bsdcampus.app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class FeedLayoutMode(val key: String, val label: String, val description: String) {
    IMMERSIVE("immersive", "Immersive Standard", "High-contrast rich feed cards"),
    STORIES("stories", "Editorial Stories", "Clean photo-centric cards"),
    DISCUSSION("discussion", "Compact Discussion", "Dense academic & Q&A format"),
    GRID("grid", "Visual Grid", "3-column Instagram gallery layout");

    companion object {
        fun fromKey(key: String?): FeedLayoutMode =
            entries.firstOrNull { it.key == key } ?: IMMERSIVE
    }
}

enum class PostImageRatio(val key: String, val label: String, val ratio: Float?) {
    FREE("free", "Original", null),
    SQUARE("1:1", "1:1", 1f),
    PORTRAIT_4_5("4:5", "4:5", 4f / 5f),
    LANDSCAPE_16_9("16:9", "16:9", 16f / 9f),
    PORTRAIT_9_16("9:16", "9:16", 9f / 16f);

    companion object {
        fun fromKey(key: String?): PostImageRatio =
            entries.firstOrNull { it.key == key } ?: FREE
    }
}

enum class CampusTab(val route: String) {
    FEED("feed"),
    EXPLORE("explore"),
    CREATE("create"),
    REELS("reels"),
    CHAT("chat"),
    ACTIVITY("activity"),
    PROFILE("profile"),
    EDIT_PROFILE("edit_profile"),
    SETTINGS("settings"),
    ADMIN("admin"),
    CONNECTIONS("connections")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class CampusUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val role: String = "student", // "student" | "teacher" | "admin"
    val grade: String? = null,
    val gradeLocked: Boolean = false,
    val avatar: String? = null,
    val bio: String? = null,
    @get:PropertyName("isApproved") @field:PropertyName("isApproved") val isApproved: Boolean = true,
    @get:PropertyName("isVerified") @field:PropertyName("isVerified") val isVerified: Boolean = false,
    @get:PropertyName("isSuspended") @field:PropertyName("isSuspended") val isSuspended: Boolean = false,
    @get:PropertyName("isBlackSociety") @field:PropertyName("isBlackSociety") val isBlackSociety: Boolean = false,
    val blackSocietyNumber: Int? = null,
    @get:PropertyName("isClassRepresentative") @field:PropertyName("isClassRepresentative") val isClassRepresentative: Boolean = false,
    val classRepresentativeGrade: Int? = null,
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @get:PropertyName("isPrivateAccount") @field:PropertyName("isPrivateAccount") val isPrivateAccount: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val lastSeenAt: Timestamp? = null,
    // Derived / merged from owner's private doc (never stored on campus_profiles):
    val email: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList()
) {
    val isAdminUser: Boolean
        get() = role == "admin" ||
            email.equals(CampusConstants.SUPER_ADMIN_EMAIL, ignoreCase = true)

    fun toProfileMap(isCreate: Boolean = false): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "uid" to uid.ifBlank { id },
            "name" to name,
            "role" to role,
            "grade" to grade,
            "gradeLocked" to gradeLocked,
            "avatar" to avatar,
            "bio" to bio,
            "isApproved" to isApproved,
            "isVerified" to isVerified,
            "isSuspended" to isSuspended,
            "isBlackSociety" to isBlackSociety,
            "blackSocietyNumber" to blackSocietyNumber?.toLong(),
            "isClassRepresentative" to isClassRepresentative,
            "classRepresentativeGrade" to classRepresentativeGrade?.toLong(),
            "isActive" to isActive,
            "isPrivateAccount" to isPrivateAccount,
            "createdAt" to if (isCreate || createdAt == null) FieldValue.serverTimestamp() else createdAt,
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastSeenAt" to if (isCreate && lastSeenAt == null) FieldValue.serverTimestamp() else lastSeenAt
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class UserPrivateInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val language: String = "English (US)",
    val feedLayoutMode: String = FeedLayoutMode.IMMERSIVE.key,
    val hideLikeCounts: Boolean = false,
    @get:PropertyName("isPrivateAccount") @field:PropertyName("isPrivateAccount") val isPrivateAccount: Boolean = false,
    val pushLikes: Boolean = true,
    val pushComments: Boolean = true,
    val pushAnnouncements: Boolean = true,
    val dataSaver: Boolean = false,
    val highQualityMedia: Boolean = true,
    val savedPostIds: List<String> = emptyList(),
    val savedReelIds: List<String> = emptyList(),
    val archivedPostIds: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(isCreate: Boolean = false): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "email" to email,
            "language" to language,
            "feedLayoutMode" to feedLayoutMode,
            "hideLikeCounts" to hideLikeCounts,
            "isPrivateAccount" to isPrivateAccount,
            "pushLikes" to pushLikes,
            "pushComments" to pushComments,
            "pushAnnouncements" to pushAnnouncements,
            "dataSaver" to dataSaver,
            "highQualityMedia" to highQualityMedia,
            "savedPostIds" to savedPostIds,
            "savedReelIds" to savedReelIds,
            "archivedPostIds" to archivedPostIds,
            "blockedUserIds" to blockedUserIds,
            "createdAt" to if (isCreate || createdAt == null) FieldValue.serverTimestamp() else createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CampusFollow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetId: String = "",
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CampusPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorRole: String = "student",
    val authorGrade: String? = null,
    val authorIsVerified: Boolean = false,
    val authorIsBlackSociety: Boolean = false,
    val authorBlackSocietyNumber: Int? = null,
    val authorIsClassRepresentative: Boolean = false,
    val authorClassRepresentativeGrade: Int? = null,
    val content: String = "",
    val category: String = "General",
    val images: List<String> = emptyList(),
    val imageAspectRatio: String = PostImageRatio.FREE.key,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicArtworkUrl: String? = null,
    val musicPreviewUrl: String? = null,
    val musicStartSec: Int = 0,
    @get:PropertyName("isOfficial") @field:PropertyName("isOfficial") val isOfficial: Boolean = false,
    @get:PropertyName("isPinned") @field:PropertyName("isPinned") val isPinned: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val viewedBy: List<String> = emptyList(),
    val commentsCount: Long = 0L,
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isProtectedAnnouncement: Boolean
        get() = isOfficial || isPinned || authorRole == "admin"

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "authorRole" to authorRole,
            "authorGrade" to authorGrade,
            "authorIsVerified" to authorIsVerified,
            "authorIsBlackSociety" to authorIsBlackSociety,
            "authorBlackSocietyNumber" to authorBlackSocietyNumber?.toLong(),
            "authorIsClassRepresentative" to authorIsClassRepresentative,
            "authorClassRepresentativeGrade" to authorClassRepresentativeGrade?.toLong(),
            "content" to content,
            "category" to category,
            "images" to images,
            "imageAspectRatio" to PostImageRatio.fromKey(imageAspectRatio).key,
            "musicTitle" to musicTitle?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "musicArtist" to musicArtist?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "musicArtworkUrl" to musicArtworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "musicPreviewUrl" to musicPreviewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "musicStartSec" to if (!musicPreviewUrl.isNullOrBlank()) musicStartSec.coerceIn(0, 60).toLong() else null,
            "isOfficial" to isOfficial,
            "isPinned" to isPinned,
            "likedBy" to likedBy,
            "viewedBy" to viewedBy,
            "commentsCount" to commentsCount,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CampusComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorRole: String = "student",
    val authorGrade: String? = null,
    val authorIsVerified: Boolean = false,
    val authorIsBlackSociety: Boolean = false,
    val authorBlackSocietyNumber: Int? = null,
    val authorIsClassRepresentative: Boolean = false,
    val authorClassRepresentativeGrade: Int? = null,
    val content: String = "",
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val replyToContent: String? = null,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "postId" to postId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "authorRole" to authorRole,
            "authorGrade" to authorGrade,
            "authorIsVerified" to authorIsVerified,
            "authorIsBlackSociety" to authorIsBlackSociety,
            "authorBlackSocietyNumber" to authorBlackSocietyNumber?.toLong(),
            "authorIsClassRepresentative" to authorIsClassRepresentative,
            "authorClassRepresentativeGrade" to authorClassRepresentativeGrade?.toLong(),
            "content" to content,
            "replyToCommentId" to replyToCommentId,
            "replyToAuthorName" to replyToAuthorName,
            "replyToContent" to replyToContent,
            "likedBy" to likedBy,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val participants: List<String> = emptyList(),
    val content: String = "",
    val mediaData: String? = null,
    val audioData: String? = null,
    val audioDurationSec: Long? = null,
    val storyId: String? = null,
    val storyAuthorName: String? = null,
    val storyMediaUrl: String? = null,
    val storyMediaType: String? = null,
    val storyCaption: String? = null,
    val replyToMessageId: String? = null,
    val replyToSenderId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null,
    val replyToMediaType: String? = null, // "text" | "image" | "video" | "audio" | "story"
    val read: Boolean = false,
    val edited: Boolean = false,
    val reactions: List<String> = emptyList(), // formatted "userId:emoji"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "recipientId" to recipientId,
            "participants" to participants,
            "content" to content,
            "mediaData" to mediaData,
            "audioData" to audioData,
            "audioDurationSec" to audioDurationSec,
            "storyId" to storyId,
            "storyAuthorName" to storyAuthorName,
            "storyMediaUrl" to storyMediaUrl,
            "storyMediaType" to storyMediaType,
            "storyCaption" to storyCaption,
            "replyToMessageId" to replyToMessageId,
            "replyToSenderId" to replyToSenderId,
            "replyToSenderName" to replyToSenderName,
            "replyToContent" to replyToContent,
            "replyToMediaType" to replyToMediaType,
            "read" to read,
            "edited" to edited,
            "reactions" to reactions,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun parsedReactions(): Map<String, List<String>> {
        val grouped = linkedMapOf<String, MutableList<String>>()
        reactions.forEach { entry ->
            val sep = entry.indexOf(':')
            if (sep > 0 && sep < entry.lastIndex) {
                val uid = entry.substring(0, sep)
                val emoji = entry.substring(sep + 1)
                grouped.getOrPut(emoji) { mutableListOf() }.add(uid)
            }
        }
        return grouped
    }
}

data class ConversationSummary(
    val otherUser: CampusUser,
    val lastMessage: DirectMessage,
    val unreadCount: Int
)

@IgnoreExtraProperties
data class CampusNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val type: String = "like", // "like" | "comment" | "follow" | "announcement" | "admin_alert"
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderAvatar" to senderAvatar,
            "type" to type,
            "title" to title,
            "message" to message,
            "read" to read,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CampusReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // "post" | "reel" | "comment" | "user" | "chat" | "app"
    val targetId: String = "",
    val targetSummary: String = "",
    val reason: String = "",
    val details: String? = null,
    val reportedMessages: List<String> = emptyList(),
    val status: String = "pending", // "pending" | "resolved" | "dismissed"
    val actionTaken: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val cleanMessages = reportedMessages
            .map { it.trim().take(400) }
            .filter { it.isNotBlank() }
            .take(5)
        val raw = mutableMapOf<String, Any?>(
            "reporterId" to reporterId,
            "targetType" to targetType,
            "targetId" to targetId,
            "targetSummary" to targetSummary,
            "reason" to reason,
            "details" to details,
            "reportedMessages" to cleanMessages.takeIf { it.isNotEmpty() },
            "status" to status,
            "actionTaken" to actionTaken,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

data class CampusAnalytics(
    val totalUsers: Int = 0,
    val studentsCount: Int = 0,
    val teachersCount: Int = 0,
    val pendingTeachersCount: Int = 0,
    val activePostsCount: Int = 0,
    val totalCommentsCount: Long = 0L,
    val pendingReportsCount: Int = 0,
    val categoryDistribution: List<Pair<String, Int>> = emptyList()
)

@IgnoreExtraProperties
data class CampusStory(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorRole: String = "student",
    val authorIsVerified: Boolean = false,
    val authorIsBlackSociety: Boolean = false,
    val authorBlackSocietyNumber: Int? = null,
    val authorIsClassRepresentative: Boolean = false,
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" | "video" | "post_card"
    val caption: String = "",
    val sharedSourceType: String? = null, // "post" | "reel"
    val sharedSourceId: String? = null,
    val sharedAuthorId: String? = null,
    val sharedAuthorName: String? = null,
    val sharedAuthorAvatar: String? = null,
    val sharedSourceCaption: String? = null,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicArtworkUrl: String? = null,
    val musicPreviewUrl: String? = null,
    val musicStartSec: Int = 0,
    val muteOriginalAudio: Boolean = false,
    val videoStartMs: Long = 0L,
    val videoEndMs: Long = 0L,
    val videoSpeed: Double = 1.0,
    val videoFilter: String? = null,
    val videoOverlaysJson: String? = null,
    val viewedBy: List<String> = emptyList(),
    val likedBy: List<String> = emptyList(),
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        val expMs = expiresAt?.toDate()?.time ?: return false
        return expMs <= nowMs
    }

    fun toCreateMap(expiresAtTimestamp: Timestamp): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "authorRole" to authorRole,
            "authorIsVerified" to authorIsVerified,
            "authorIsBlackSociety" to authorIsBlackSociety,
            "authorBlackSocietyNumber" to authorBlackSocietyNumber?.toLong(),
            "authorIsClassRepresentative" to authorIsClassRepresentative,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "sharedSourceType" to sharedSourceType,
            "sharedSourceId" to sharedSourceId,
            "sharedAuthorId" to sharedAuthorId,
            "sharedAuthorName" to sharedAuthorName,
            "sharedAuthorAvatar" to sharedAuthorAvatar,
            "sharedSourceCaption" to sharedSourceCaption,
            "musicTitle" to musicTitle,
            "musicArtist" to musicArtist,
            "musicArtworkUrl" to musicArtworkUrl,
            "musicPreviewUrl" to musicPreviewUrl,
            "musicStartSec" to if (!musicPreviewUrl.isNullOrBlank()) musicStartSec.toLong() else null,
            "muteOriginalAudio" to if (muteOriginalAudio) true else null,
            "videoStartMs" to if (videoStartMs > 0L) videoStartMs else null,
            "videoEndMs" to if (videoEndMs > 0L) videoEndMs else null,
            "videoSpeed" to if (videoSpeed != 1.0) videoSpeed else null,
            "videoFilter" to videoFilter?.takeIf { it.isNotBlank() && it != "ORIGINAL" },
            "videoOverlaysJson" to videoOverlaysJson?.takeIf { it.isNotBlank() },
            "viewedBy" to viewedBy,
            "likedBy" to likedBy,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAtTimestamp
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

data class StoryGroup(
    val author: CampusUser,
    val stories: List<CampusStory>,
    val hasUnseen: Boolean,
    val isOwn: Boolean
)

@IgnoreExtraProperties
data class CampusReel(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorRole: String = "student",
    val authorGrade: String? = null,
    val authorIsVerified: Boolean = false,
    val authorIsBlackSociety: Boolean = false,
    val authorBlackSocietyNumber: Int? = null,
    val authorIsClassRepresentative: Boolean = false,
    val authorClassRepresentativeGrade: Int? = null,
    val videoUrl: String = "",
    val videoAspectRatio: String = PostImageRatio.PORTRAIT_9_16.key,
    val caption: String = "",
    val category: String = "General",
    val audioTitle: String = "Original audio",
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicArtworkUrl: String? = null,
    val musicPreviewUrl: String? = null,
    val musicStartSec: Int = 0,
    val muteOriginalAudio: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val viewedBy: List<String> = emptyList(),
    val commentsCount: Long = 0L,
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "authorRole" to authorRole,
            "authorGrade" to authorGrade,
            "authorIsVerified" to authorIsVerified,
            "authorIsBlackSociety" to authorIsBlackSociety,
            "authorBlackSocietyNumber" to authorBlackSocietyNumber?.toLong(),
            "authorIsClassRepresentative" to authorIsClassRepresentative,
            "authorClassRepresentativeGrade" to authorClassRepresentativeGrade?.toLong(),
            "videoUrl" to videoUrl,
            "videoAspectRatio" to PostImageRatio.fromKey(videoAspectRatio).key,
            "caption" to caption.trim().take(2000),
            "category" to category,
            "audioTitle" to audioTitle.ifBlank { "Original audio" }.take(120),
            "musicTitle" to musicTitle?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "musicArtist" to musicArtist?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "musicArtworkUrl" to musicArtworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "musicPreviewUrl" to musicPreviewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "musicStartSec" to if (!musicPreviewUrl.isNullOrBlank()) musicStartSec.coerceIn(0, 60).toLong() else null,
            "muteOriginalAudio" to muteOriginalAudio,
            "likedBy" to likedBy,
            "viewedBy" to viewedBy,
            "commentsCount" to commentsCount,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class ReelComment(
    @DocumentId val id: String = "",
    val reelId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorRole: String = "student",
    val authorGrade: String? = null,
    val authorIsVerified: Boolean = false,
    val authorIsBlackSociety: Boolean = false,
    val authorBlackSocietyNumber: Int? = null,
    val authorIsClassRepresentative: Boolean = false,
    val authorClassRepresentativeGrade: Int? = null,
    val content: String = "",
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val replyToContent: String? = null,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "reelId" to reelId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "authorRole" to authorRole,
            "authorGrade" to authorGrade,
            "authorIsVerified" to authorIsVerified,
            "authorIsBlackSociety" to authorIsBlackSociety,
            "authorBlackSocietyNumber" to authorBlackSocietyNumber?.toLong(),
            "authorIsClassRepresentative" to authorIsClassRepresentative,
            "authorClassRepresentativeGrade" to authorClassRepresentativeGrade?.toLong(),
            "content" to content.trim().take(1000),
            "replyToCommentId" to replyToCommentId,
            "replyToAuthorName" to replyToAuthorName,
            "replyToContent" to replyToContent,
            "likedBy" to likedBy,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class HighlightMediaItem(
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "mediaUrl" to mediaUrl,
        "mediaType" to if (mediaType == "video") "video" else "image",
        "caption" to caption.take(300)
    )
}

@IgnoreExtraProperties
data class StoryHighlight(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val coverAvatar: String? = null,
    val coverEmoji: String? = null,
    val items: List<HighlightMediaItem> = emptyList(),
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "ownerId" to ownerId,
            "title" to title.trim().take(40),
            "coverAvatar" to coverAvatar,
            "coverEmoji" to coverEmoji,
            "items" to items.take(25).map { it.toMap() },
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

enum class NoteBubbleColor(
    val key: String,
    val label: String,
    val containerArgb: Long,
    val textArgb: Long,
    val borderArgb: Long
) {
    PINK("pink", "Pink", 0xFFFF7EB6L, 0xFF2B0818L, 0xFFE1306CL),
    DARK_PINK("dark_pink", "Dark Pink", 0xFFAD1457L, 0xFFFFFFFFL, 0xFFF472B6L),
    BLUE("blue", "Blue", 0xFF0095F6L, 0xFFFFFFFFL, 0xFF7DD3FCL),
    PURPLE("purple", "Purple", 0xFF7E22CEL, 0xFFFFFFFFL, 0xFFD8B4FEL),
    EMERALD("emerald", "Teal", 0xFF0F766EL, 0xFFFFFFFFL, 0xFF5EEAD4L),
    AMBER("amber", "Gold", 0xFFF59E0BL, 0xFF1C1917L, 0xFFB45309L),
    DARK("dark", "Dark", 0xFF1F1F26L, 0xFFFFFFFFL, 0xFF3F3F46L);

    companion object {
        fun fromKey(key: String?): NoteBubbleColor =
            entries.firstOrNull { it.key == key } ?: PINK
    }
}

@IgnoreExtraProperties
data class CampusNote(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val text: String = "",
    val bubbleColor: String = NoteBubbleColor.PINK.key,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicArtworkUrl: String? = null,
    val musicPreviewUrl: String? = null,
    val musicStartSec: Int = 0,
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        val expMs = expiresAt?.toDate()?.time ?: return false
        return expMs <= nowMs
    }

    fun toCreateMap(expiresAtTimestamp: Timestamp): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName.trim().take(80),
            "authorAvatar" to authorAvatar,
            "text" to text.trim().take(100),
            "bubbleColor" to NoteBubbleColor.fromKey(bubbleColor).key,
            "musicTitle" to musicTitle?.trim()?.take(120),
            "musicArtist" to musicArtist?.trim()?.take(120),
            "musicArtworkUrl" to musicArtworkUrl?.trim()?.take(500),
            "musicPreviewUrl" to musicPreviewUrl?.trim()?.take(500),
            "musicStartSec" to if (!musicPreviewUrl.isNullOrBlank()) musicStartSec.coerceIn(0, 60).toLong() else null,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAtTimestamp
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class AppUpdateConfig(
    @DocumentId val id: String = "update",
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    @get:PropertyName("isMandatory") @field:PropertyName("isMandatory") val isMandatory: Boolean = false,
    val updatedBy: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(updatedByUid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "latestVersionCode" to latestVersionCode.coerceIn(1, 100000).toLong(),
            "latestVersionName" to latestVersionName.trim().take(40),
            "apkUrl" to apkUrl.trim().take(1000),
            "releaseNotes" to releaseNotes.trim().take(1000),
            "isMandatory" to isMandatory,
            "updatedBy" to updatedByUid.take(128),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

enum class ChatBackgroundPreset(
    val key: String,
    val label: String,
    val colorsArgb: List<Long>
) {
    DEFAULT("default", "Default", emptyList()),
    MIDNIGHT("midnight", "Midnight", listOf(0xFF0F172AL, 0xFF1E1B4BL)),
    AURORA("aurora", "Aurora", listOf(0xFF064E3BL, 0xFF0F172AL, 0xFF312E81L)),
    ROSE_DUSK("rose", "Rose Dusk", listOf(0xFF4C0519L, 0xFF831843L, 0xFF3B0764L)),
    EMERALD("emerald", "Emerald", listOf(0xFF022C22L, 0xFF115E59L)),
    SUNSET("sunset", "Sunset", listOf(0xFF7C2D12L, 0xFF9D174DL, 0xFF4C1D95L)),
    SKY_SOFT("sky", "Soft Sky", listOf(0xFFE0F2FEL, 0xFFEDE9FEL)),
    LAVENDER("lavender", "Lavender", listOf(0xFFF3E8FFL, 0xFFFCE7F3L));

    val isDarkPreset: Boolean
        get() = this != DEFAULT && this != SKY_SOFT && this != LAVENDER

    companion object {
        fun fromKey(key: String?): ChatBackgroundPreset =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

@IgnoreExtraProperties
data class ChatPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val peerId: String = "",
    val nickname: String? = null,
    val backgroundPreset: String = ChatBackgroundPreset.DEFAULT.key,
    val backgroundImage: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "peerId" to peerId,
            "nickname" to nickname?.trim()?.takeIf { it.isNotBlank() }?.take(40),
            "backgroundPreset" to ChatBackgroundPreset.fromKey(backgroundPreset).key,
            "backgroundImage" to backgroundImage?.trim()?.takeIf { it.isNotBlank() }?.take(350_000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

