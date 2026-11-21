package com.bsdcampus.app.notifications

import android.content.Context
import android.util.Log
import com.bsdcampus.app.R
import com.bsdcampus.app.model.CampusNotification
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.model.UserPrivateInfo
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object CampusNotificationCoordinator {
    private const val TAG = "CampusNotifCoordinator"

    private var appContext: Context? = null
    private var activeUid: String? = null

    private var messagesRegistration: ListenerRegistration? = null
    private var notificationsRegistration: ListenerRegistration? = null
    private var privateInfoRegistration: ListenerRegistration? = null
    private var profilesRegistration: ListenerRegistration? = null
    private var notifScope: CoroutineScope? = null

    private val profileCache = ConcurrentHashMap<String, CampusUser>()
    @Volatile
    private var privatePrefs: UserPrivateInfo? = null

    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var activeChatPeerId: String? = null

    @Volatile
    var isActivityTabOpen: Boolean = false

    private val _inAppBanners = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val inAppBanners: SharedFlow<String> = _inAppBanners.asSharedFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val ctx = appContext ?: return@AuthStateListener
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            stopListeners()
            CampusNotificationWorker.cancelPeriodic(ctx)
        } else if (uid != activeUid) {
            startListeners(ctx, uid)
            CampusNotificationWorker.schedulePeriodic(ctx)
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        NotificationHelper.ensureChannels(context.applicationContext)
        Firebase.auth.addAuthStateListener(authListener)
        Firebase.auth.currentUser?.uid?.let { uid ->
            startListeners(context.applicationContext, uid)
            CampusNotificationWorker.schedulePeriodic(context.applicationContext)
        }
    }

    @Synchronized
    private fun startListeners(context: Context, uid: String) {
        stopListeners()
        activeUid = uid
        notifScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))

        profilesRegistration = db.collection("campus_profiles")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                snapshot.toObjects(
                    CampusUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).forEach { user ->
                    if (user.uid.isNotBlank()) profileCache[user.uid] = user
                }
            }

        privateInfoRegistration = db.collection("users").document(uid)
            .collection("private").document("info")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                privatePrefs = snapshot.toObject(
                    UserPrivateInfo::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

        messagesRegistration = db.collection("messages")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Message listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val msgs = snapshot.toObjects(
                    DirectMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val allIds = msgs.map { it.id }.filter { it.isNotBlank() }
                val newIds = CampusNotificationStore.consumeUnseenMessageIds(context, uid, allIds).toHashSet()
                if (newIds.isEmpty()) return@addSnapshotListener

                msgs.filter { msg ->
                    msg.id in newIds && msg.recipientId == uid && !msg.read
                }.forEach { msg ->
                    val viewingSameChat = isAppInForeground && activeChatPeerId == msg.senderId
                    if (!viewingSameChat) {
                        val sender = profileCache[msg.senderId]
                        val senderName = sender?.let {
                            if (it.isBlackSociety) "Member - ${it.blackSocietyNumber ?: 1}" else it.name
                        } ?: "Squadify Message"
                        val senderAvatar = if (sender?.isBlackSociety == true) null else sender?.avatar
                        val preview = when {
                            !msg.storyId.isNullOrBlank() -> "Replied to your story: ${msg.content}"
                            !msg.audioData.isNullOrBlank() -> "Sent a voice note"
                            !msg.mediaData.isNullOrBlank() && msg.content.isBlank() -> "Sent a photo"
                            else -> msg.content
                        }
                        if (isAppInForeground) {
                            _inAppBanners.tryEmit("$senderName: $preview")
                        }
                        notifScope?.launch {
                            NotificationHelper.showChatNotification(
                                context = context,
                                messageId = msg.id,
                                senderId = msg.senderId,
                                senderName = senderName,
                                senderAvatar = senderAvatar,
                                previewText = preview
                            )
                        }
                    }
                }
            }

        notificationsRegistration = db.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Activity listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.toObjects(
                    CampusNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val allIds = items.map { it.id }.filter { it.isNotBlank() }
                val newIds = CampusNotificationStore.consumeUnseenActivityIds(context, uid, allIds).toHashSet()
                if (newIds.isEmpty()) return@addSnapshotListener

                val prefs = privatePrefs
                items.filter { notif -> notif.id in newIds && !notif.read }.forEach { notif ->
                    val enabled = when (notif.type) {
                        "like" -> prefs?.pushLikes != false
                        "comment" -> prefs?.pushComments != false
                        "announcement", "admin_alert" -> prefs?.pushAnnouncements != false
                        else -> true
                    }
                    if (enabled) {
                        val actSender = profileCache[notif.senderId]
                        val actSenderName = when {
                            actSender?.isBlackSociety == true -> "Member - ${actSender.blackSocietyNumber ?: 1}"
                            actSender != null -> actSender.name.ifBlank { notif.senderName.ifBlank { "Squadify" } }
                            else -> notif.senderName.ifBlank { "Squadify" }
                        }
                        val actSenderAvatar = when {
                            actSender?.isBlackSociety == true -> null
                            actSender != null -> actSender.avatar
                            else -> notif.senderAvatar
                        }
                        if (isAppInForeground && !isActivityTabOpen) {
                            _inAppBanners.tryEmit(notif.title.ifBlank { notif.message })
                        }
                        notifScope?.launch {
                            NotificationHelper.showActivityNotification(
                                context = context,
                                notificationId = notif.id,
                                senderId = notif.senderId,
                                senderName = actSenderName,
                                senderAvatar = actSenderAvatar,
                                title = notif.title.ifBlank { "Squadify" },
                                message = notif.message
                            )
                        }
                    }
                }
            }
    }

    @Synchronized
    private fun stopListeners() {
        activeUid = null
        notifScope?.cancel()
        notifScope = null
        messagesRegistration?.remove()
        notificationsRegistration?.remove()
        privateInfoRegistration?.remove()
        profilesRegistration?.remove()
        messagesRegistration = null
        notificationsRegistration = null
        privateInfoRegistration = null
        profilesRegistration = null
        privatePrefs = null
        profileCache.clear()
    }
}
