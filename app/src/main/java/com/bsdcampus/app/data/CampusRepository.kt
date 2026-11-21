package com.bsdcampus.app.data

import android.content.Context
import com.bsdcampus.app.R
import com.bsdcampus.app.firebase.OperationType
import com.bsdcampus.app.firebase.handleFirestoreError
import com.bsdcampus.app.model.AppUpdateConfig
import com.bsdcampus.app.model.CampusComment
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusFollow
import com.bsdcampus.app.model.CampusNote
import com.bsdcampus.app.model.CampusNotification
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusReel
import com.bsdcampus.app.model.CampusReport
import com.bsdcampus.app.model.CampusStory
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.model.HighlightMediaItem
import com.bsdcampus.app.model.NoteBubbleColor
import com.bsdcampus.app.model.PostImageRatio
import com.bsdcampus.app.model.ReelComment
import com.bsdcampus.app.model.StoryHighlight
import com.bsdcampus.app.model.UserPrivateInfo
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class CampusRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private val recordedPostViews = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val recordedReelViews = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfiles(): Flow<List<CampusUser>> = flow {
        requireUserId()
        val path = "campus_profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePrivateInfo(uid: String): Flow<UserPrivateInfo?> = flow {
        requireUserId()
        val path = "users/$uid/private/info"
        emitAll(
            db.document(path)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPrivateInfo::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeIsAdminMarker(uid: String): Flow<Boolean> = flow {
        requireUserId()
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snapshot -> snapshot.exists() }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path)
                    emit(false)
                }
        )
    }

    fun observeFollows(): Flow<List<CampusFollow>> = flow {
        requireUserId()
        val path = "campus_follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusFollow::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePosts(): Flow<List<CampusPost>> = flow {
        requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CampusPost> { it.isPinned }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun fetchLatestPostsFromServer(): Result<List<CampusPost>> {
        requireUserId()
        val path = "posts"
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("isActive", true)
                .get(Source.SERVER)
                .await()
            val posts = snapshot.toObjects(
                CampusPost::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(
                compareByDescending<CampusPost> { it.isPinned }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logStreamError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchLatestStoriesFromServer(): Result<List<CampusStory>> {
        requireUserId()
        val path = "stories"
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("isActive", true)
                .get(Source.SERVER)
                .await()
            val now = System.currentTimeMillis()
            val stories = snapshot.toObjects(
                CampusStory::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { !it.isExpired(now) }
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            Result.success(stories)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logStreamError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeStories(): Flow<List<CampusStory>> = flow {
        requireUserId()
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(
                        CampusStory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isExpired(now) }
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeHighlights(): Flow<List<StoryHighlight>> = flow {
        requireUserId()
        val path = "story_highlights"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StoryHighlight::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCampusNotes(): Flow<List<CampusNote>> = flow {
        requireUserId()
        val path = "campus_notes"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(
                        CampusNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { !it.isExpired(now) }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeComments(postId: String): Flow<List<CampusComment>> = flow {
        requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeReels(): Flow<List<CampusReel>> = flow {
        requireUserId()
        val path = "reels"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusReel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeReelComments(reelId: String): Flow<List<ReelComment>> = flow {
        requireUserId()
        val path = "reels/$reelId/comments"
        emitAll(
            db.collection("reels").document(reelId).collection("comments")
                .whereEqualTo("reelId", reelId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReelComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeNotifications(uid: String): Flow<List<CampusNotification>> = flow {
        requireUserId()
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMessages(uid: String): Flow<List<DirectMessage>> = flow {
        requireUserId()
        val path = "messages"
        emitAll(
            db.collection(path)
                .whereArrayContains("participants", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DirectMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTypingStatus(uid: String): Flow<Map<String, Boolean>> = flow {
        requireUserId()
        val path = "typing_status"
        emitAll(
            db.collection(path)
                .whereArrayContains("participants", uid)
                .snapshots()
                .map { snapshot ->
                    val map = mutableMapOf<String, Boolean>()
                    snapshot.documents.forEach { doc ->
                        val senderId = doc.getString("senderId") ?: return@forEach
                        val recipientId = doc.getString("recipientId") ?: return@forEach
                        val isTyping = doc.getBoolean("isTyping") ?: false
                        if (recipientId == uid) {
                            map[senderId] = isTyping
                        }
                    }
                    map
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeReports(isAdmin: Boolean, uid: String): Flow<List<CampusReport>> = flow {
        requireUserId()
        val path = "reports"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("reporterId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getAllPrivateEmailsForAdmin(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        return try {
            val profiles = db.collection("campus_profiles").whereEqualTo("isActive", true).get().await()
            for (doc in profiles.documents) {
                try {
                    val info = db.collection("users").document(doc.id).collection("private").document("info").get().await()
                    val email = info.getString("email")
                    if (!email.isNullOrBlank()) result[doc.id] = email
                } catch (_: Exception) {
                }
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            result
        }
    }

    suspend fun registerProfile(
        name: String,
        email: String,
        role: String,
        grade: String?,
        avatarUrl: String?
    ): Result<CampusUser> {
        val uid = requireUserId()
        val authUser = auth.currentUser
        val verifiedAuthEmail = authUser?.email.orEmpty()
        val isSuperAdmin = verifiedAuthEmail.equals(CampusConstants.SUPER_ADMIN_EMAIL, ignoreCase = true) &&
            authUser?.isEmailVerified == true
        val effectiveRole = if (isSuperAdmin) "admin" else if (role == "teacher") "teacher" else "student"
        val isApproved = isSuperAdmin || effectiveRole == "student"

        val profileDoc = db.collection("campus_profiles").document(uid)
        val privateDoc = db.collection("users").document(uid).collection("private").document("info")
        val adminDoc = db.collection("admins").document(uid)

        val user = CampusUser(
            id = uid,
            uid = uid,
            name = name.trim(),
            role = effectiveRole,
            grade = grade?.takeIf { it.isNotBlank() },
            gradeLocked = !grade.isNullOrBlank() && !isSuperAdmin,
            avatar = avatarUrl?.takeIf { it.isNotBlank() } ?: CampusConstants.defaultAvatarFor(name),
            bio = if (isSuperAdmin) "Squadify Administrator" else "",
            isApproved = isApproved,
            isVerified = isSuperAdmin,
            isSuspended = false,
            isBlackSociety = false,
            isClassRepresentative = false,
            isActive = true,
            email = email
        )

        val privateInfo = UserPrivateInfo(
            id = "info",
            userId = uid,
            email = email.ifBlank { "$uid@bsd.edu" }
        )

        return try {
            if (isSuperAdmin) {
                adminDoc.set(
                    mapOf(
                        "uid" to uid,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val existingProfileSnap = profileDoc.get().await()
            val resolvedUser = if (existingProfileSnap.exists()) {
                val current = existingProfileSnap.toObject(CampusUser::class.java)
                if (isSuperAdmin && current != null) {
                    profileDoc.update(
                        mapOf(
                            "role" to "admin",
                            "isApproved" to true,
                            "isVerified" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    current.copy(role = "admin", isApproved = true, isVerified = true, email = email)
                } else {
                    current?.copy(email = email) ?: user
                }
            } else {
                profileDoc.set(user.toProfileMap(isCreate = true)).await()
                user
            }
            val existingPrivateSnap = privateDoc.get().await()
            if (!existingPrivateSnap.exists()) {
                privateDoc.set(privateInfo.toMap(isCreate = true)).await()
            }
            Result.success(resolvedUser)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, profileDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(
        currentUser: CampusUser,
        isAdmin: Boolean,
        newName: String,
        newGrade: String?,
        newAvatar: String?,
        newBio: String,
        context: Context? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val profileDoc = db.collection("campus_profiles").document(uid)
        val finalGrade = if (currentUser.gradeLocked && !isAdmin) {
            currentUser.grade
        } else {
            newGrade?.takeIf { it.isNotBlank() }
        }
        val finalGradeLocked = currentUser.gradeLocked || (!finalGrade.isNullOrBlank() && !isAdmin)

        val trimmedNewAvatar = newAvatar?.trim()?.takeIf { it.isNotBlank() }
        val updates = mutableMapOf<String, Any>(
            "name" to newName.trim().take(80),
            "bio" to newBio.trim().take(200),
            "gradeLocked" to finalGradeLocked,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!finalGrade.isNullOrBlank()) updates["grade"] = finalGrade
        if (trimmedNewAvatar != null) updates["avatar"] = trimmedNewAvatar

        return try {
            val oldAvatar = currentUser.avatar?.trim()?.takeIf { it.isNotBlank() }
            profileDoc.update(updates).await()
            if (trimmedNewAvatar != null && oldAvatar != null && oldAvatar != trimmedNewAvatar) {
                cleanupAndPropagateAvatarChange(
                    uid = uid,
                    oldAvatar = oldAvatar,
                    newAvatar = trimmedNewAvatar,
                    isAdmin = isAdmin,
                    context = context
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnAvatar(
        newAvatar: String,
        isAdmin: Boolean = false,
        context: Context? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmed = newAvatar.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Selected avatar is empty."))
        }
        if (trimmed.length > 350_000) {
            return Result.failure(IllegalArgumentException("Avatar exceeds 350KB Firestore field limit."))
        }
        val profileDoc = db.collection("campus_profiles").document(uid)
        return try {
            val oldAvatar = runCatching { profileDoc.get().await().getString("avatar")?.trim() }.getOrNull()
            profileDoc.update(
                mapOf(
                    "avatar" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (!oldAvatar.isNullOrBlank() && oldAvatar != trimmed) {
                cleanupAndPropagateAvatarChange(
                    uid = uid,
                    oldAvatar = oldAvatar,
                    newAvatar = trimmed,
                    isAdmin = isAdmin,
                    context = context
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileDoc.path)
            Result.failure(e)
        }
    }

    private suspend fun cleanupAndPropagateAvatarChange(
        uid: String,
        oldAvatar: String,
        newAvatar: String?,
        isAdmin: Boolean,
        context: Context?
    ) {
        CampusMediaRepository.evictCachedImage(context, oldAvatar)
        if (MediaUtils.isChunkedMediaUri(oldAvatar)) {
            runCatching {
                CampusMediaRepository.deleteMediaAssetsForUris(
                    uris = listOf(oldAvatar),
                    isAdmin = isAdmin,
                    db = db,
                    auth = auth,
                    context = context
                )
            }
        }
        runCatching {
            val sentNotifs = db.collection("notifications")
                .whereEqualTo("senderId", uid)
                .get()
                .await()
            val toUpdate = sentNotifs.documents.filter { doc ->
                doc.getString("senderAvatar") != newAvatar
            }
            val avatarValue: Any = if (!newAvatar.isNullOrBlank()) newAvatar else FieldValue.delete()
            toUpdate.chunked(400).forEach { group ->
                val batch = db.batch()
                group.forEach { doc ->
                    batch.update(doc.reference, mapOf("senderAvatar" to avatarValue))
                }
                batch.commit().await()
            }
        }
    }

    suspend fun updateLastSeen(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val profileDoc = db.collection("campus_profiles").document(uid)
        return try {
            profileDoc.update(
                mapOf(
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Ignore NOT_FOUND cleanly while onboarding profile is not created yet
            val firestoreErr = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreErr?.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun upsertCampusNote(
        author: CampusUser,
        text: String,
        bubbleColor: NoteBubbleColor,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmedText = text.trim().take(100)
        val hasSong = musicTrack != null && musicTrack.previewUrl.isNotBlank()
        if (trimmedText.isBlank() && !hasSong) {
            return Result.failure(IllegalArgumentException("Enter a note message or choose a song."))
        }
        val docRef = db.collection("campus_notes").document(uid)
        val displayAuthorName = if (author.isBlackSociety) {
            "Member - ${author.blackSocietyNumber ?: 1}"
        } else {
            author.name.ifBlank { "BSD Member" }
        }
        val note = CampusNote(
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            text = trimmedText,
            bubbleColor = bubbleColor.key,
            musicTitle = musicTrack?.title?.take(120),
            musicArtist = musicTrack?.artist?.take(120),
            musicArtworkUrl = musicTrack?.artworkUrl?.takeIf { it.isNotBlank() }?.take(500),
            musicPreviewUrl = musicTrack?.previewUrl?.takeIf { it.isNotBlank() }?.take(500),
            musicStartSec = if (hasSong) musicStartSec.coerceIn(0, 60) else 0,
            isActive = true
        )
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
        return try {
            docRef.set(note.toCreateMap(expiresAt)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCampusNote(): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("campus_notes").document(uid)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePrivateSettings(info: UserPrivateInfo): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("private").document("info")
        val profileRef = db.collection("campus_profiles").document(uid)
        return try {
            docRef.set(info.copy(userId = uid).toMap(isCreate = info.createdAt == null)).await()
            try {
                profileRef.update(
                    mapOf(
                        "isPrivateAccount" to info.isPrivateAccount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val firestoreErr = generateSequence<Throwable>(e) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreErr?.code != FirebaseFirestoreException.Code.NOT_FOUND) {
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) return Result.success(Unit)
        val followId = "${uid}_$targetUserId"
        val docRef = db.collection("campus_follows").document(followId)
        return try {
            if (currentlyFollowing) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "followerId" to uid,
                        "targetId" to targetUserId,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendNotification(
                    recipientId = targetUserId,
                    actor = actor,
                    type = "follow",
                    title = "New Follower",
                    message = "${actor.name} started following your campus updates."
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        author: CampusUser,
        isAdmin: Boolean,
        content: String,
        category: String,
        images: List<String>,
        imageAspectRatio: String = PostImageRatio.FREE.key,
        isOfficial: Boolean,
        isPinned: Boolean,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(UUID.randomUUID().toString())
        val allowOfficial = isAdmin && isOfficial
        val allowPinned = isAdmin && isPinned
        val safeAuthorRole = when {
            isAdmin -> "admin"
            author.role == "teacher" -> "teacher"
            else -> "student"
        }
        val displayAuthorName = if (author.isBlackSociety) {
            "Member - ${author.blackSocietyNumber ?: 1}"
        } else author.name
        val cleanPreviewUrl = musicTrack?.previewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)

        val post = CampusPost(
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            authorRole = safeAuthorRole,
            authorGrade = author.grade,
            authorIsVerified = author.isVerified || isAdmin || author.isClassRepresentative,
            authorIsBlackSociety = author.isBlackSociety,
            authorBlackSocietyNumber = author.blackSocietyNumber,
            authorIsClassRepresentative = author.isClassRepresentative,
            authorClassRepresentativeGrade = author.classRepresentativeGrade,
            content = content.trim(),
            category = category,
            images = if (isAdmin) images else images.take(10),
            imageAspectRatio = PostImageRatio.fromKey(imageAspectRatio).key,
            musicTitle = if (cleanPreviewUrl != null) musicTrack?.title?.trim()?.takeIf { it.isNotBlank() }?.take(120) else null,
            musicArtist = if (cleanPreviewUrl != null) musicTrack?.artist?.trim()?.takeIf { it.isNotBlank() }?.take(120) else null,
            musicArtworkUrl = if (cleanPreviewUrl != null) musicTrack?.artworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500) else null,
            musicPreviewUrl = cleanPreviewUrl,
            musicStartSec = if (cleanPreviewUrl != null) musicStartSec.coerceIn(0, 60) else 0,
            isOfficial = allowOfficial,
            isPinned = allowPinned,
            likedBy = emptyList(),
            commentsCount = 0L,
            isActive = true
        )

        return try {
            docRef.set(post.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikePost(post: CampusPost, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(post.id)
        val alreadyLiked = post.likedBy.contains(uid)
        val updatedLikes = if (alreadyLiked) post.likedBy - uid else (post.likedBy + uid).distinct()
        return try {
            docRef.update(
                mapOf(
                    "likedBy" to updatedLikes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (!alreadyLiked && post.authorId != uid) {
                sendNotification(
                    recipientId = post.authorId,
                    actor = actor,
                    type = "like",
                    title = "Liked your post",
                    message = "${actor.name} liked your ${post.category} post."
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markPostViewed(post: CampusPost): Result<Unit> {
        val uid = requireUserId()
        if (post.id.isBlank() || post.viewedBy.contains(uid) || !recordedPostViews.add("${post.id}:$uid")) {
            return Result.success(Unit)
        }
        val docRef = db.collection("posts").document(post.id)
        return try {
            docRef.update("viewedBy", FieldValue.arrayUnion(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            recordedPostViews.remove("${post.id}:$uid")
            throw e
        } catch (e: Exception) {
            recordedPostViews.remove("${post.id}:$uid")
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(post: CampusPost, isAdmin: Boolean, appContext: Context? = null): Result<Unit> {
        val uid = requireUserId()
        if (!isAdmin && (post.authorId != uid || post.isProtectedAnnouncement)) {
            return Result.failure(SecurityException("Only administrators can delete official announcements."))
        }
        val docRef = db.collection("posts").document(post.id)
        return try {
            docRef.delete().await()
            if (post.images.isNotEmpty()) {
                CampusMediaRepository.deleteMediaAssetsForUris(
                    uris = post.images,
                    isAdmin = isAdmin,
                    db = db,
                    auth = auth,
                    context = appContext
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        post: CampusPost,
        isAdmin: Boolean,
        newContent: String,
        newCategory: String,
        newImageAspectRatio: String = post.imageAspectRatio,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0
    ): Result<Unit> {
        val uid = requireUserId()
        if (!isAdmin && (post.authorId != uid || post.isProtectedAnnouncement)) {
            return Result.failure(SecurityException("Only the post author or an administrator can edit this post."))
        }
        val docRef = db.collection("posts").document(post.id)
        val cleanPreviewUrl = musicTrack?.previewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        val updates = mutableMapOf<String, Any>(
            "content" to newContent.trim().take(2000),
            "category" to newCategory,
            "imageAspectRatio" to PostImageRatio.fromKey(newImageAspectRatio).key,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (cleanPreviewUrl != null) {
            updates["musicPreviewUrl"] = cleanPreviewUrl
            updates["musicTitle"] = musicTrack?.title?.trim()?.takeIf { it.isNotBlank() }?.take(120) ?: "Track"
            updates["musicArtist"] = musicTrack?.artist?.trim()?.takeIf { it.isNotBlank() }?.take(120) ?: "Artist"
            val artwork = musicTrack?.artworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
            updates["musicArtworkUrl"] = artwork ?: FieldValue.delete()
            updates["musicStartSec"] = musicStartSec.coerceIn(0, 60).toLong()
        } else {
            updates["musicTitle"] = FieldValue.delete()
            updates["musicArtist"] = FieldValue.delete()
            updates["musicArtworkUrl"] = FieldValue.delete()
            updates["musicPreviewUrl"] = FieldValue.delete()
            updates["musicStartSec"] = FieldValue.delete()
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePinPost(post: CampusPost, isAdmin: Boolean): Result<Unit> {
        requireUserId()
        if (!isAdmin) {
            return Result.failure(SecurityException("Only administrators can pin or unpin posts."))
        }
        val docRef = db.collection("posts").document(post.id)
        return try {
            docRef.update(
                mapOf(
                    "isPinned" to !post.isPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        author: CampusUser,
        isAdmin: Boolean,
        mediaUrl: String,
        mediaType: String,
        caption: String,
        sharedSourceType: String? = null,
        sharedSourceId: String? = null,
        sharedAuthorId: String? = null,
        sharedAuthorName: String? = null,
        sharedAuthorAvatar: String? = null,
        sharedSourceCaption: String? = null,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false,
        videoStartMs: Long = 0L,
        videoEndMs: Long = 0L,
        videoSpeed: Double = 1.0,
        videoFilter: String? = null,
        videoOverlaysJson: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("stories").document(UUID.randomUUID().toString())
        val safeAuthorRole = when {
            isAdmin -> "admin"
            author.role == "teacher" -> "teacher"
            else -> "student"
        }
        val displayAuthorName = if (author.isBlackSociety) {
            "Member - ${author.blackSocietyNumber ?: 1}"
        } else author.name
        val safeMediaType = when (mediaType) {
            "video" -> "video"
            "post_card" -> "post_card"
            else -> "image"
        }
        val safeSourceType = sharedSourceType?.takeIf { it == "post" || it == "reel" }
        val cleanPreviewUrl = musicTrack?.previewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
        val story = CampusStory(
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            authorRole = safeAuthorRole,
            authorIsVerified = author.isVerified || isAdmin || author.isClassRepresentative,
            authorIsBlackSociety = author.isBlackSociety,
            authorBlackSocietyNumber = author.blackSocietyNumber,
            authorIsClassRepresentative = author.isClassRepresentative,
            mediaUrl = mediaUrl.take(350_000),
            mediaType = safeMediaType,
            caption = caption.trim().take(300),
            sharedSourceType = safeSourceType,
            sharedSourceId = sharedSourceId?.takeIf { it.isNotBlank() }?.take(128),
            sharedAuthorId = sharedAuthorId?.takeIf { it.isNotBlank() }?.take(128),
            sharedAuthorName = sharedAuthorName?.takeIf { it.isNotBlank() }?.take(80),
            sharedAuthorAvatar = sharedAuthorAvatar?.takeIf { it.isNotBlank() && it.length <= 350_000 },
            sharedSourceCaption = sharedSourceCaption?.takeIf { it.isNotBlank() }?.take(600),
            musicTitle = if (cleanPreviewUrl != null) musicTrack?.title?.trim()?.take(120) else null,
            musicArtist = if (cleanPreviewUrl != null) musicTrack?.artist?.trim()?.take(120) else null,
            musicArtworkUrl = if (cleanPreviewUrl != null) musicTrack?.artworkUrl?.trim()?.take(500) else null,
            musicPreviewUrl = cleanPreviewUrl,
            musicStartSec = if (cleanPreviewUrl != null) musicStartSec.coerceIn(0, 60) else 0,
            muteOriginalAudio = muteOriginalAudio,
            videoStartMs = videoStartMs.coerceIn(0L, 3_600_000L),
            videoEndMs = videoEndMs.coerceIn(0L, 3_600_000L),
            videoSpeed = videoSpeed.coerceIn(0.25, 3.0),
            videoFilter = videoFilter?.trim()?.takeIf { it.isNotBlank() && it != "ORIGINAL" }?.take(30),
            videoOverlaysJson = videoOverlaysJson?.takeIf { it.isNotBlank() }?.take(8000),
            viewedBy = emptyList(),
            isActive = true,
            expiresAt = expiresAt
        )
        return try {
            docRef.set(story.toCreateMap(expiresAt)).await()
            if (!sharedAuthorId.isNullOrBlank() && sharedAuthorId != uid && safeSourceType != null) {
                sendNotification(
                    recipientId = sharedAuthorId,
                    actor = author,
                    type = "like",
                    title = "Shared to Story",
                    message = "${displayAuthorName} added your $safeSourceType to their story."
                )
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markStoryViewed(story: CampusStory): Result<Unit> {
        val uid = requireUserId()
        if (story.authorId == uid || story.viewedBy.contains(uid)) return Result.success(Unit)
        val docRef = db.collection("stories").document(story.id)
        return try {
            docRef.update("viewedBy", FieldValue.arrayUnion(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(story: CampusStory, isAdmin: Boolean, appContext: Context? = null): Result<Unit> {
        val uid = requireUserId()
        if (!isAdmin && story.authorId != uid) {
            return Result.failure(SecurityException("Only the story author or an administrator can delete this story."))
        }
        val docRef = db.collection("stories").document(story.id)
        return try {
            docRef.delete().await()
            if (story.sharedSourceId.isNullOrBlank()) {
                CampusMediaRepository.deleteMediaAssetsForUris(
                    uris = listOf(story.mediaUrl),
                    isAdmin = isAdmin,
                    db = db,
                    auth = auth,
                    context = appContext
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeStory(story: CampusStory, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("stories").document(story.id)
        val wasLiked = story.likedBy.contains(uid)
        val updatedLikes = if (wasLiked) {
            story.likedBy.filterNot { it == uid }
        } else {
            (story.likedBy + uid).distinct()
        }
        return try {
            docRef.update("likedBy", updatedLikes).await()
            if (!wasLiked && story.authorId != uid) {
                sendNotification(
                    recipientId = story.authorId,
                    actor = actor,
                    type = "like",
                    title = "Story Liked",
                    message = "${actor.name} liked your story"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createHighlight(
        title: String,
        coverAvatar: String?,
        coverEmoji: String?,
        items: List<HighlightMediaItem>
    ): Result<String> {
        val uid = requireUserId()
        val trimmedTitle = title.trim().take(40)
        if (trimmedTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Highlight title cannot be empty."))
        }
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one photo or video for this highlight."))
        }
        val docRef = db.collection("story_highlights").document(UUID.randomUUID().toString())
        val highlight = StoryHighlight(
            ownerId = uid,
            title = trimmedTitle,
            coverAvatar = coverAvatar?.takeIf { it.isNotBlank() && it.length <= 350_000 },
            coverEmoji = coverEmoji?.takeIf { it.isNotBlank() }?.take(16),
            items = items.take(25),
            isActive = true
        )
        return try {
            docRef.set(highlight.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateHighlight(
        highlightId: String,
        title: String,
        coverAvatar: String?,
        coverEmoji: String?,
        items: List<HighlightMediaItem>
    ): Result<Unit> {
        requireUserId()
        val trimmedTitle = title.trim().take(40)
        if (trimmedTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Highlight title cannot be empty."))
        }
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one photo or video for this highlight."))
        }
        val docRef = db.collection("story_highlights").document(highlightId)
        val updates = mutableMapOf<String, Any>(
            "title" to trimmedTitle,
            "items" to items.take(25).map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        updates["coverAvatar"] = coverAvatar?.takeIf { it.isNotBlank() && it.length <= 350_000 } ?: FieldValue.delete()
        updates["coverEmoji"] = coverEmoji?.takeIf { it.isNotBlank() }?.take(16) ?: FieldValue.delete()
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHighlight(highlightId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("story_highlights").document(highlightId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: CampusPost,
        author: CampusUser,
        content: String,
        isAdmin: Boolean,
        replyToComment: CampusComment? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmed = content.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty"))

        val commentRef = db.collection("posts").document(post.id)
            .collection("comments").document(UUID.randomUUID().toString())
        val postRef = db.collection("posts").document(post.id)

        val safeAuthorRole = when {
            isAdmin -> "admin"
            author.role == "teacher" -> "teacher"
            else -> "student"
        }
        val displayAuthorName = if (author.isBlackSociety) "Member - ${author.blackSocietyNumber ?: 1}" else author.name
        val replyTargetName = replyToComment?.let { parent ->
            if (parent.authorIsBlackSociety) "Member - ${parent.authorBlackSocietyNumber ?: 1}" else parent.authorName
        }?.take(80)

        val comment = CampusComment(
            postId = post.id,
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            authorRole = safeAuthorRole,
            authorGrade = author.grade,
            authorIsVerified = author.isVerified || isAdmin || author.isClassRepresentative,
            authorIsBlackSociety = author.isBlackSociety,
            authorBlackSocietyNumber = author.blackSocietyNumber,
            authorIsClassRepresentative = author.isClassRepresentative,
            authorClassRepresentativeGrade = author.classRepresentativeGrade,
            content = trimmed,
            replyToCommentId = replyToComment?.id?.takeIf { it.isNotBlank() }?.take(128),
            replyToAuthorName = replyTargetName,
            replyToContent = replyToComment?.content?.trim()?.take(300),
            likedBy = emptyList()
        )

        return try {
            val batch = db.batch()
            batch.set(commentRef, comment.toCreateMap())
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            if (post.authorId != uid) {
                sendNotification(
                    recipientId = post.authorId,
                    actor = author,
                    type = "comment",
                    title = if (replyToComment?.authorId == post.authorId) "Replied to Comment" else "New Comment",
                    message = "$displayAuthorName commented: \"${trimmed.take(60)}\""
                )
            }
            if (replyToComment != null && replyToComment.authorId != uid && replyToComment.authorId != post.authorId) {
                sendNotification(
                    recipientId = replyToComment.authorId,
                    actor = author,
                    type = "comment",
                    title = "New Reply",
                    message = "$displayAuthorName replied to your comment: \"${trimmed.take(60)}\""
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeComment(postId: String, comment: CampusComment, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(postId).collection("comments").document(comment.id)
        val alreadyLiked = comment.likedBy.contains(uid)
        val updatedLikes = if (alreadyLiked) comment.likedBy - uid else (comment.likedBy + uid).distinct()
        val actorDisplayName = if (actor.isBlackSociety) "Member - ${actor.blackSocietyNumber ?: 1}" else actor.name
        return try {
            docRef.update("likedBy", updatedLikes).await()
            if (!alreadyLiked && comment.authorId != uid) {
                sendNotification(
                    recipientId = comment.authorId,
                    actor = actor,
                    type = "like",
                    title = "Liked your comment",
                    message = "$actorDisplayName liked your comment: \"${comment.content.take(50)}\""
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        requireUserId()
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        val postRef = db.collection("posts").document(postId)
        return try {
            val batch = db.batch()
            batch.delete(commentRef)
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun createReel(
        author: CampusUser,
        isAdmin: Boolean,
        videoUrl: String,
        videoAspectRatio: String = PostImageRatio.PORTRAIT_9_16.key,
        caption: String,
        category: String,
        audioTitle: String,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        if (videoUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Please select a video for your reel."))
        }
        val docRef = db.collection("reels").document(UUID.randomUUID().toString())
        val safeAuthorRole = when {
            isAdmin -> "admin"
            author.role == "teacher" -> "teacher"
            else -> "student"
        }
        val displayAuthorName = if (author.isBlackSociety) {
            "Member - ${author.blackSocietyNumber ?: 1}"
        } else author.name
        val cleanPreviewUrl = musicTrack?.previewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)

        val reel = CampusReel(
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            authorRole = safeAuthorRole,
            authorGrade = author.grade,
            authorIsVerified = author.isVerified || isAdmin || author.isClassRepresentative,
            authorIsBlackSociety = author.isBlackSociety,
            authorBlackSocietyNumber = author.blackSocietyNumber,
            authorIsClassRepresentative = author.isClassRepresentative,
            authorClassRepresentativeGrade = author.classRepresentativeGrade,
            videoUrl = videoUrl.take(350_000),
            videoAspectRatio = PostImageRatio.fromKey(videoAspectRatio).key,
            caption = caption.trim().take(2000),
            category = category,
            audioTitle = audioTitle.trim().ifBlank { "Original audio" }.take(120),
            musicTitle = if (cleanPreviewUrl != null) musicTrack?.title?.trim()?.takeIf { it.isNotBlank() }?.take(120) else null,
            musicArtist = if (cleanPreviewUrl != null) musicTrack?.artist?.trim()?.takeIf { it.isNotBlank() }?.take(120) else null,
            musicArtworkUrl = if (cleanPreviewUrl != null) musicTrack?.artworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500) else null,
            musicPreviewUrl = cleanPreviewUrl,
            musicStartSec = if (cleanPreviewUrl != null) musicStartSec.coerceIn(0, 60) else 0,
            muteOriginalAudio = muteOriginalAudio,
            likedBy = emptyList(),
            commentsCount = 0L,
            isActive = true
        )
        return try {
            docRef.set(reel.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeReel(reel: CampusReel, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("reels").document(reel.id)
        val alreadyLiked = reel.likedBy.contains(uid)
        val updatedLikes = if (alreadyLiked) reel.likedBy - uid else (reel.likedBy + uid).distinct()
        return try {
            docRef.update(
                mapOf(
                    "likedBy" to updatedLikes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (!alreadyLiked && reel.authorId != uid) {
                sendNotification(
                    recipientId = reel.authorId,
                    actor = actor,
                    type = "like",
                    title = "Liked your reel",
                    message = "${actor.name} liked your ${reel.category} reel."
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markReelViewed(reel: CampusReel): Result<Unit> {
        val uid = requireUserId()
        if (reel.id.isBlank() || reel.viewedBy.contains(uid) || !recordedReelViews.add("${reel.id}:$uid")) {
            return Result.success(Unit)
        }
        val docRef = db.collection("reels").document(reel.id)
        return try {
            docRef.update("viewedBy", FieldValue.arrayUnion(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            recordedReelViews.remove("${reel.id}:$uid")
            throw e
        } catch (e: Exception) {
            recordedReelViews.remove("${reel.id}:$uid")
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReel(
        reel: CampusReel,
        isAdmin: Boolean,
        newCaption: String,
        newCategory: String,
        newAudioTitle: String,
        newVideoAspectRatio: String = reel.videoAspectRatio,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        if (!isAdmin && reel.authorId != uid) {
            return Result.failure(SecurityException("Only the reel creator or an administrator can edit this reel."))
        }
        val docRef = db.collection("reels").document(reel.id)
        val cleanPreviewUrl = musicTrack?.previewUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        val updates = mutableMapOf<String, Any>(
            "caption" to newCaption.trim().take(2000),
            "category" to newCategory,
            "audioTitle" to newAudioTitle.trim().ifBlank { "Original audio" }.take(120),
            "videoAspectRatio" to PostImageRatio.fromKey(newVideoAspectRatio).key,
            "muteOriginalAudio" to muteOriginalAudio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (cleanPreviewUrl != null) {
            updates["musicPreviewUrl"] = cleanPreviewUrl
            updates["musicTitle"] = musicTrack?.title?.trim()?.takeIf { it.isNotBlank() }?.take(120) ?: "Track"
            updates["musicArtist"] = musicTrack?.artist?.trim()?.takeIf { it.isNotBlank() }?.take(120) ?: "Artist"
            val artwork = musicTrack?.artworkUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
            updates["musicArtworkUrl"] = artwork ?: FieldValue.delete()
            updates["musicStartSec"] = musicStartSec.coerceIn(0, 60).toLong()
        } else {
            updates["musicTitle"] = FieldValue.delete()
            updates["musicArtist"] = FieldValue.delete()
            updates["musicArtworkUrl"] = FieldValue.delete()
            updates["musicPreviewUrl"] = FieldValue.delete()
            updates["musicStartSec"] = FieldValue.delete()
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReel(reel: CampusReel, isAdmin: Boolean, appContext: Context? = null): Result<Unit> {
        val uid = requireUserId()
        if (!isAdmin && reel.authorId != uid) {
            return Result.failure(SecurityException("Only the reel creator or an administrator can delete this reel."))
        }
        val docRef = db.collection("reels").document(reel.id)
        return try {
            docRef.delete().await()
            CampusMediaRepository.deleteMediaAssetsForUris(
                uris = listOf(reel.videoUrl),
                isAdmin = isAdmin,
                db = db,
                auth = auth,
                context = appContext
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addReelComment(
        reel: CampusReel,
        author: CampusUser,
        content: String,
        isAdmin: Boolean,
        replyToComment: ReelComment? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmed = content.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty"))

        val commentRef = db.collection("reels").document(reel.id)
            .collection("comments").document(UUID.randomUUID().toString())
        val reelRef = db.collection("reels").document(reel.id)

        val safeAuthorRole = when {
            isAdmin -> "admin"
            author.role == "teacher" -> "teacher"
            else -> "student"
        }
        val displayAuthorName = if (author.isBlackSociety) "Member - ${author.blackSocietyNumber ?: 1}" else author.name
        val replyTargetName = replyToComment?.let { parent ->
            if (parent.authorIsBlackSociety) "Member - ${parent.authorBlackSocietyNumber ?: 1}" else parent.authorName
        }?.take(80)

        val comment = ReelComment(
            reelId = reel.id,
            authorId = uid,
            authorName = displayAuthorName,
            authorAvatar = if (author.isBlackSociety) null else author.avatar,
            authorRole = safeAuthorRole,
            authorGrade = author.grade,
            authorIsVerified = author.isVerified || isAdmin || author.isClassRepresentative,
            authorIsBlackSociety = author.isBlackSociety,
            authorBlackSocietyNumber = author.blackSocietyNumber,
            authorIsClassRepresentative = author.isClassRepresentative,
            authorClassRepresentativeGrade = author.classRepresentativeGrade,
            content = trimmed,
            replyToCommentId = replyToComment?.id?.takeIf { it.isNotBlank() }?.take(128),
            replyToAuthorName = replyTargetName,
            replyToContent = replyToComment?.content?.trim()?.take(300),
            likedBy = emptyList()
        )

        return try {
            val batch = db.batch()
            batch.set(commentRef, comment.toCreateMap())
            batch.update(
                reelRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            if (reel.authorId != uid) {
                sendNotification(
                    recipientId = reel.authorId,
                    actor = author,
                    type = "comment",
                    title = if (replyToComment?.authorId == reel.authorId) "Replied to Reel Comment" else "Reel Comment",
                    message = "$displayAuthorName commented on your reel: \"${trimmed.take(60)}\""
                )
            }
            if (replyToComment != null && replyToComment.authorId != uid && replyToComment.authorId != reel.authorId) {
                sendNotification(
                    recipientId = replyToComment.authorId,
                    actor = author,
                    type = "comment",
                    title = "New Reel Reply",
                    message = "$displayAuthorName replied to your comment: \"${trimmed.take(60)}\""
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeReelComment(reelId: String, comment: ReelComment, actor: CampusUser): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("reels").document(reelId).collection("comments").document(comment.id)
        val alreadyLiked = comment.likedBy.contains(uid)
        val updatedLikes = if (alreadyLiked) comment.likedBy - uid else (comment.likedBy + uid).distinct()
        val actorDisplayName = if (actor.isBlackSociety) "Member - ${actor.blackSocietyNumber ?: 1}" else actor.name
        return try {
            docRef.update("likedBy", updatedLikes).await()
            if (!alreadyLiked && comment.authorId != uid) {
                sendNotification(
                    recipientId = comment.authorId,
                    actor = actor,
                    type = "like",
                    title = "Liked your comment",
                    message = "$actorDisplayName liked your reel comment: \"${comment.content.take(50)}\""
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReelComment(reelId: String, commentId: String): Result<Unit> {
        requireUserId()
        val commentRef = db.collection("reels").document(reelId).collection("comments").document(commentId)
        val reelRef = db.collection("reels").document(reelId)
        return try {
            val batch = db.batch()
            batch.delete(commentRef)
            batch.update(
                reelRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        recipientId: String,
        content: String,
        mediaData: String? = null,
        audioData: String? = null,
        audioDurationSec: Long? = null,
        replyStory: CampusStory? = null,
        replyStoryAuthorName: String? = null,
        replyToMessage: DirectMessage? = null,
        replyToSenderName: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("messages").document(UUID.randomUUID().toString())
        val safeStoryMediaUrl = replyStory?.mediaUrl?.takeIf { it.length <= 350_000 }
        val replyMediaType = replyToMessage?.let { target ->
            when {
                !target.storyId.isNullOrBlank() -> "story"
                !target.audioData.isNullOrBlank() -> "audio"
                MediaUtils.isVideoMediaUri(target.mediaData) -> "video"
                !target.mediaData.isNullOrBlank() -> "image"
                else -> "text"
            }
        }
        val replyPreviewContent = replyToMessage?.let { target ->
            target.content.ifBlank {
                when (replyMediaType) {
                    "story" -> "Story reply"
                    "audio" -> "Voice message (${target.audioDurationSec ?: 0}s)"
                    "video" -> "Video"
                    "image" -> "Photo"
                    else -> ""
                }
            }.take(500)
        }
        val msg = DirectMessage(
            senderId = uid,
            recipientId = recipientId,
            participants = listOf(uid, recipientId),
            content = content.trim(),
            mediaData = mediaData,
            audioData = audioData,
            audioDurationSec = audioDurationSec,
            storyId = replyStory?.id?.takeIf { it.isNotBlank() },
            storyAuthorName = replyStoryAuthorName?.take(80),
            storyMediaUrl = safeStoryMediaUrl,
            storyMediaType = replyStory?.mediaType?.takeIf { it == "image" || it == "video" || it == "post_card" },
            storyCaption = replyStory?.caption?.take(300),
            replyToMessageId = replyToMessage?.id?.takeIf { it.isNotBlank() },
            replyToSenderId = replyToMessage?.senderId?.takeIf { it.isNotBlank() },
            replyToSenderName = replyToSenderName?.take(80),
            replyToContent = replyPreviewContent,
            replyToMediaType = replyMediaType,
            read = false,
            edited = false,
            reactions = emptyList()
        )
        return try {
            docRef.set(msg.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("messages").document(messageId)
        return try {
            docRef.update(
                mapOf(
                    "content" to newContent.trim(),
                    "edited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("messages").document(messageId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageReaction(message: DirectMessage, emoji: String): Result<Unit> {
        val uid = requireUserId()
        val token = "$uid:$emoji"
        val updated = if (message.reactions.contains(token)) {
            message.reactions - token
        } else {
            (message.reactions + token).takeLast(50)
        }
        val docRef = db.collection("messages").document(message.id)
        return try {
            docRef.update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesRead(unreadMessages: List<DirectMessage>): Result<Unit> {
        val uid = requireUserId()
        val targets = unreadMessages.filter { it.recipientId == uid && !it.read }
        if (targets.isEmpty()) return Result.success(Unit)
        return try {
            val batch = db.batch()
            targets.forEach { msg ->
                batch.update(
                    db.collection("messages").document(msg.id),
                    mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "messages")
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(recipientId: String, isTyping: Boolean) {
        val uid = requireUserId()
        val pairId = "${uid}_$recipientId"
        val docRef = db.collection("typing_status").document(pairId)
        try {
            docRef.set(
                mapOf(
                    "senderId" to uid,
                    "recipientId" to recipientId,
                    "participants" to listOf(uid, recipientId),
                    "isTyping" to isTyping,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    private suspend fun sendNotification(
        recipientId: String,
        actor: CampusUser,
        type: String,
        title: String,
        message: String
    ) {
        val uid = requireUserId()
        val notifRef = db.collection("notifications").document(UUID.randomUUID().toString())
        val notif = CampusNotification(
            recipientId = recipientId,
            senderId = uid,
            senderName = actor.name.ifBlank { "Squadify" },
            senderAvatar = actor.avatar,
            type = type,
            title = title.take(120),
            message = message.take(400),
            read = false
        )
        try {
            notifRef.set(notif.toCreateMap()).await()
        } catch (_: Exception) {
        }
    }

    suspend fun markAllNotificationsRead(notifications: List<CampusNotification>): Result<Unit> {
        requireUserId()
        val unread = notifications.filter { !it.read && it.id.isNotBlank() }
        if (unread.isEmpty()) return Result.success(Unit)
        return try {
            unread.chunked(400).forEach { group ->
                val batch = db.batch()
                group.forEach { n ->
                    batch.update(db.collection("notifications").document(n.id), mapOf("read" to true))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetSummary: String,
        reason: String,
        details: String?
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("reports").document(UUID.randomUUID().toString())
        val report = CampusReport(
            reporterId = uid,
            targetType = targetType,
            targetId = targetId,
            targetSummary = targetSummary.take(300),
            reason = reason.take(160),
            details = details?.trim()?.take(1000),
            status = "pending"
        )
        return try {
            docRef.set(report.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Admin Actions ---

    suspend fun claimSuperAdmin(user: CampusUser): Result<Unit> {
        val uid = requireUserId()
        return try {
            db.collection("admins").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "email" to (auth.currentUser?.email ?: user.email),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("campus_profiles").document(uid).update(
                mapOf(
                    "role" to "admin",
                    "isApproved" to true,
                    "isVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "campus_profiles/$uid")
            Result.failure(e)
        }
    }

    private suspend fun adminUpdateProfileFields(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("campus_profiles").document(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Campus profile not found for user."))
            }
            val data = snap.data?.toMutableMap() ?: mutableMapOf()
            data["uid"] = (data["uid"] as? String)?.takeIf { it.isNotBlank() } ?: userId
            data["name"] = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: "BSD Member"
            data["role"] = (data["role"] as? String)?.takeIf { it in listOf("student", "teacher", "admin") } ?: "student"
            if (data["isApproved"] !is Boolean) data["isApproved"] = true
            if (data["isVerified"] !is Boolean) data["isVerified"] = false
            if (data["isSuspended"] !is Boolean) data["isSuspended"] = false
            if (data["isBlackSociety"] !is Boolean) data["isBlackSociety"] = false
            if (data["isClassRepresentative"] !is Boolean) data["isClassRepresentative"] = false
            if (data["isActive"] !is Boolean) data["isActive"] = true
            if (data["createdAt"] !is Timestamp) data["createdAt"] = FieldValue.serverTimestamp()
            data.putAll(updates)
            data["updatedAt"] = FieldValue.serverTimestamp()
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun approveTeacher(teacherId: String): Result<Unit> {
        return adminUpdateProfileFields(
            teacherId,
            mapOf(
                "isApproved" to true,
                "isVerified" to true
            )
        )
    }

    suspend fun rejectTeacher(teacherId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("campus_profiles").document(teacherId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSuspendUser(userId: String, suspend: Boolean): Result<Unit> {
        return adminUpdateProfileFields(
            userId,
            mapOf("isSuspended" to suspend)
        )
    }

    suspend fun toggleVerifyUser(userId: String, verify: Boolean): Result<Unit> {
        return adminUpdateProfileFields(
            userId,
            mapOf("isVerified" to verify)
        )
    }

    suspend fun updateUserRole(userId: String, email: String, newRole: String): Result<Unit> {
        requireUserId()
        return try {
            if (newRole == "admin") {
                db.collection("admins").document(userId).set(
                    mapOf("uid" to userId, "email" to email, "createdAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                try {
                    db.collection("admins").document(userId).delete().await()
                } catch (_: Exception) {
                }
            }
            adminUpdateProfileFields(
                userId,
                mapOf(
                    "role" to newRole,
                    "isApproved" to true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "campus_profiles/$userId")
            Result.failure(e)
        }
    }

    suspend fun toggleDarkSociety(userId: String, grant: Boolean, allUsers: List<CampusUser>): Result<Unit> {
        val nextNum = if (grant) {
            val maxNum = allUsers.mapNotNull { it.blackSocietyNumber }.maxOrNull() ?: 0
            (maxNum + 1).coerceAtLeast(1)
        } else null

        val updates = mutableMapOf<String, Any>("isBlackSociety" to grant)
        if (nextNum != null) {
            updates["blackSocietyNumber"] = nextNum.toLong()
        }
        return adminUpdateProfileFields(userId, updates)
    }

    suspend fun assignClassRepresentative(userId: String, gradeNum: Int, assign: Boolean): Result<Unit> {
        requireUserId()
        val safeGrade = gradeNum.coerceIn(1, 12)
        if (assign) {
            try {
                val previousHolders = db.collection("campus_profiles")
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("classRepresentativeGrade", safeGrade.toLong())
                    .get()
                    .await()
                for (doc in previousHolders.documents) {
                    if (doc.id != userId && doc.getBoolean("isClassRepresentative") == true) {
                        adminUpdateProfileFields(
                            doc.id,
                            mapOf(
                                "isClassRepresentative" to false,
                                "classRepresentativeGrade" to 0L
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        val updates = if (assign) {
            mapOf(
                "isClassRepresentative" to true,
                "classRepresentativeGrade" to safeGrade.toLong(),
                "isVerified" to true
            )
        } else {
            mapOf(
                "isClassRepresentative" to false,
                "classRepresentativeGrade" to 0L
            )
        }
        return adminUpdateProfileFields(userId, updates)
    }

    suspend fun updateReportStatus(reportId: String, status: String, actionTaken: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("reports").document(reportId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "actionTaken" to actionTaken,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun broadcastAnnouncement(
        adminUser: CampusUser,
        isAdmin: Boolean,
        content: String,
        category: String,
        allUsers: List<CampusUser>
    ): Result<Unit> {
        if (!isAdmin) {
            return Result.failure(SecurityException("Only administrators can broadcast official notices."))
        }
        val postResult = createPost(
            author = adminUser,
            isAdmin = true,
            content = content,
            category = category,
            images = emptyList(),
            isOfficial = true,
            isPinned = true
        )
        if (postResult.isFailure) return Result.failure(postResult.exceptionOrNull()!!)

        val uid = requireUserId()
        try {
            val recipients = allUsers.filter { it.uid != uid }.take(40)
            recipients.forEach { u ->
                sendNotification(
                    recipientId = u.uid,
                    actor = adminUser,
                    type = "announcement",
                    title = "Official Squadify Notice",
                    message = content.take(120)
                )
            }
        } catch (_: Exception) {
        }
        return Result.success(Unit)
    }

    // --- In-App Update Config ---

    fun observeAppUpdateConfig(): Flow<AppUpdateConfig?> = flow {
        requireUserId()
        val path = "app_config/update"
        emitAll(
            db.document(path)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            AppUpdateConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path)
                    emit(null)
                }
        )
    }

    suspend fun fetchAppUpdateConfigFromServer(): Result<AppUpdateConfig?> {
        requireUserId()
        val docRef = db.collection("app_config").document("update")
        return try {
            val snap = docRef.get(Source.SERVER).await()
            val cfg = if (snap.exists()) {
                snap.toObject(
                    AppUpdateConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(cfg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logStreamError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishAppUpdateConfig(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        isMandatory: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanUrl = apkUrl.trim()
        if (!cleanUrl.startsWith("https://", ignoreCase = true) &&
            !cleanUrl.startsWith("http://", ignoreCase = true)
        ) {
            return Result.failure(IllegalArgumentException("APK URL must start with https:// or http://"))
        }
        val cleanName = versionName.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a valid version name (e.g. 2.6.0)."))
        }
        val docRef = db.collection("app_config").document("update")
        val cfg = AppUpdateConfig(
            id = "update",
            latestVersionCode = versionCode.coerceIn(1, 100000),
            latestVersionName = cleanName,
            apkUrl = cleanUrl,
            releaseNotes = releaseNotes,
            isMandatory = isMandatory
        )
        return try {
            docRef.set(cfg.toWriteMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: error
        if (auth.currentUser != null) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
