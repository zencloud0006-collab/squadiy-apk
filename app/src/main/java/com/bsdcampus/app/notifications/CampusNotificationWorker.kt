package com.bsdcampus.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bsdcampus.app.R
import com.bsdcampus.app.firebase.configureAppCheck
import com.bsdcampus.app.model.CampusNotification
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.model.UserPrivateInfo
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class CampusNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        configureAppCheck(applicationContext, null)
        NotificationHelper.ensureChannels(applicationContext)

        val isOneMinuteLoop = inputData.getBoolean(KEY_IS_ONE_MINUTE_LOOP, false)
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            cancelPeriodic(applicationContext)
            return Result.success()
        }
        val db = FirebaseFirestore.getInstance(applicationContext.getString(R.string.firestore_database_id))

        val result = try {
            val prefs = try {
                val snap = db.collection("users").document(uid)
                    .collection("private").document("info").get().await()
                if (snap.exists()) {
                    snap.toObject(UserPrivateInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            } catch (_: Exception) {
                null
            }

            // 1. Check unread incoming direct messages
            val msgSnap = db.collection("messages")
                .whereArrayContains("participants", uid)
                .get()
                .await()
            val msgs = msgSnap.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val newMsgIds = CampusNotificationStore.consumeUnseenMessageIds(
                applicationContext,
                uid,
                msgs.map { it.id }.filter { it.isNotBlank() }
            ).toHashSet()

            val senderNames = mutableMapOf<String, String>()
            val senderAvatars = mutableMapOf<String, String?>()

            val incomingMsgs = msgs.filter { it.id in newMsgIds && it.recipientId == uid && !it.read }
            if (incomingMsgs.isNotEmpty()) {
                incomingMsgs.map { it.senderId }.distinct().forEach { senderId ->
                    try {
                        val userDoc = db.collection("campus_profiles").document(senderId).get().await()
                        val user = userDoc.toObject(CampusUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (user != null) {
                            senderNames[senderId] = if (user.isBlackSociety) {
                                "Member - ${user.blackSocietyNumber ?: 1}"
                            } else {
                                user.name
                            }
                            senderAvatars[senderId] = if (user.isBlackSociety) null else user.avatar
                        }
                    } catch (_: Exception) {
                    }
                }
                incomingMsgs.forEach { msg ->
                    val viewingSameChat = CampusNotificationCoordinator.isAppInForeground &&
                        CampusNotificationCoordinator.activeChatPeerId == msg.senderId
                    if (!viewingSameChat) {
                        val senderName = senderNames[msg.senderId] ?: "Squadify Message"
                        val preview = when {
                            !msg.storyId.isNullOrBlank() -> "Replied to your story: ${msg.content}"
                            !msg.audioData.isNullOrBlank() -> "Sent a voice note"
                            !msg.mediaData.isNullOrBlank() && msg.content.isBlank() -> "Sent a photo"
                            else -> msg.content
                        }
                        NotificationHelper.showChatNotification(
                            context = applicationContext,
                            messageId = msg.id,
                            senderId = msg.senderId,
                            senderName = senderName,
                            senderAvatar = senderAvatars[msg.senderId],
                            previewText = preview
                        )
                    }
                }
            }

            // 2. Check unread activity notifications
            val notifSnap = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .get()
                .await()
            val notifs = notifSnap.toObjects(CampusNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val newNotifIds = CampusNotificationStore.consumeUnseenActivityIds(
                applicationContext,
                uid,
                notifs.map { it.id }.filter { it.isNotBlank() }
            ).toHashSet()

            val unreadNotifs = notifs.filter { it.id in newNotifIds && !it.read }
            unreadNotifs.map { it.senderId }
                .filter { it.isNotBlank() && it !in senderNames }
                .distinct()
                .forEach { senderId ->
                    try {
                        val userDoc = db.collection("campus_profiles").document(senderId).get().await()
                        val user = userDoc.toObject(CampusUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (user != null) {
                            senderNames[senderId] = if (user.isBlackSociety) {
                                "Member - ${user.blackSocietyNumber ?: 1}"
                            } else {
                                user.name
                            }
                            senderAvatars[senderId] = if (user.isBlackSociety) null else user.avatar
                        }
                    } catch (_: Exception) {
                    }
                }

            unreadNotifs.forEach { notif ->
                val enabled = when (notif.type) {
                    "like" -> prefs?.pushLikes != false
                    "comment" -> prefs?.pushComments != false
                    "announcement", "admin_alert" -> prefs?.pushAnnouncements != false
                    else -> true
                }
                if (enabled) {
                    val actSenderName = senderNames[notif.senderId]?.takeIf { it.isNotBlank() }
                        ?: notif.senderName.ifBlank { "Squadify" }
                    val actSenderAvatar = if (notif.senderId in senderNames) {
                        senderAvatars[notif.senderId]
                    } else {
                        notif.senderAvatar
                    }
                    NotificationHelper.showActivityNotification(
                        context = applicationContext,
                        notificationId = notif.id,
                        senderId = notif.senderId,
                        senderName = actSenderName,
                        senderAvatar = actSenderAvatar,
                        title = notif.title.ifBlank { "Squadify" },
                        message = notif.message
                    )
                }
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Background notification check failed: ${e.message}")
            if (isOneMinuteLoop) Result.success() else Result.retry()
        }

        if (Firebase.auth.currentUser != null) {
            scheduleOneMinuteLoop(
                context = applicationContext,
                appendAfterCurrent = isOneMinuteLoop
            )
        }
        return result
    }

    companion object {
        private const val TAG = "CampusNotifWorker"
        private const val PERIODIC_WORK_NAME = "bsd_campus_periodic_notifications"
        internal const val ONE_MINUTE_SYNC_WORK_NAME = "bsd_campus_one_minute_notifications"
        private const val IMMEDIATE_WORK_NAME = "bsd_campus_immediate_notifications"
        private const val KEY_IS_ONE_MINUTE_LOOP = "is_one_minute_loop"
        const val SYNC_INTERVAL_MINUTES = 1L

        private fun workManagerOrNull(context: Context): WorkManager? {
            val appCtx = context.applicationContext
            return try {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appCtx, Configuration.Builder().build())
                }
                WorkManager.getInstance(appCtx)
            } catch (t: Throwable) {
                Log.w(TAG, "WorkManager unavailable: ${t.message}")
                null
            }
        }

        fun schedulePeriodic(context: Context) {
            val wm = workManagerOrNull(context) ?: return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val watchdogRequest = PeriodicWorkRequestBuilder<CampusNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                watchdogRequest
            )
            scheduleOneMinuteLoop(context = context, appendAfterCurrent = false)
        }

        private fun scheduleOneMinuteLoop(context: Context, appendAfterCurrent: Boolean) {
            val wm = workManagerOrNull(context) ?: return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val nextRequest = OneTimeWorkRequestBuilder<CampusNotificationWorker>()
                .setInitialDelay(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_IS_ONE_MINUTE_LOOP to true))
                .build()
            wm.enqueueUniqueWork(
                ONE_MINUTE_SYNC_WORK_NAME,
                if (appendAfterCurrent) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                nextRequest
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val wm = workManagerOrNull(context) ?: return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CampusNotificationWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_IS_ONE_MINUTE_LOOP to false))
                .build()
            wm.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            val wm = workManagerOrNull(context) ?: return
            wm.cancelUniqueWork(ONE_MINUTE_SYNC_WORK_NAME)
            wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }
}
