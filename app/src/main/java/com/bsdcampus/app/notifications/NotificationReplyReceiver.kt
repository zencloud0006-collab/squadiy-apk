package com.bsdcampus.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.bsdcampus.app.R
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.firebase.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifIntId = intent.getIntExtra(EXTRA_NOTIFICATION_INT_ID, 0)
        val recipientId = intent.getStringExtra(EXTRA_RECIPIENT_ID).orEmpty()
        val recipientName = intent.getStringExtra(EXTRA_RECIPIENT_NAME).orEmpty().ifBlank { "BSD Member" }
        val recipientAvatar = intent.getStringExtra(EXTRA_RECIPIENT_AVATAR)
        val incomingPreview = intent.getStringExtra(EXTRA_INCOMING_PREVIEW).orEmpty()
        val messageDocId = intent.getStringExtra(EXTRA_MESSAGE_DOC_ID)
        val activityDocId = intent.getStringExtra(EXTRA_ACTIVITY_DOC_ID)

        if (action == ACTION_MARK_READ) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val uid = Firebase.auth.currentUser?.uid
                    if (uid != null) {
                        val db = FirebaseFirestore.getInstance(
                            context.applicationContext.getString(R.string.firestore_database_id)
                        )
                        if (!messageDocId.isNullOrBlank()) {
                            runCatching {
                                db.collection("messages").document(messageDocId)
                                    .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
                                    .await()
                            }
                        }
                        if (!activityDocId.isNullOrBlank()) {
                            runCatching {
                                db.collection("notifications").document(activityDocId)
                                    .update(mapOf("read" to true))
                                    .await()
                            }
                        }
                    }
                    if (notifIntId != 0) {
                        NotificationManagerCompat.from(context).cancel(notifIntId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action != ACTION_REPLY) return
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (replyText.isBlank() || recipientId.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                configureAppCheck(context.applicationContext, null)
                val auth = Firebase.auth
                if (auth.currentUser == null) return@launch
                val db = FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
                val repo = CampusRepository(db, auth)
                val sendRes = repo.sendMessage(recipientId = recipientId, content = replyText)

                if (!messageDocId.isNullOrBlank()) {
                    runCatching {
                        db.collection("messages").document(messageDocId)
                            .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
                            .await()
                    }
                }
                if (!activityDocId.isNullOrBlank()) {
                    runCatching {
                        db.collection("notifications").document(activityDocId)
                            .update(mapOf("read" to true))
                            .await()
                    }
                }

                if (sendRes.isSuccess && notifIntId != 0 && NotificationHelper.hasNotificationPermission(context)) {
                    val avatarBitmap = NotificationHelper.loadCircularSenderBitmap(
                        context = context.applicationContext,
                        avatarUrl = recipientAvatar,
                        senderName = recipientName
                    )
                    val mePerson = Person.Builder()
                        .setKey("me")
                        .setName("You")
                        .build()
                    val senderPerson = Person.Builder()
                        .setKey(recipientId)
                        .setName(recipientName)
                        .setIcon(IconCompat.createWithBitmap(avatarBitmap))
                        .build()

                    val style = NotificationCompat.MessagingStyle(mePerson)
                    if (incomingPreview.isNotBlank()) {
                        style.addMessage(incomingPreview, System.currentTimeMillis() - 1000L, senderPerson)
                    }
                    style.addMessage(replyText, System.currentTimeMillis(), mePerson)

                    val updated = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_CHAT_ID)
                        .setSmallIcon(R.drawable.ic_notification_bsd)
                        .setLargeIcon(avatarBitmap)
                        .setStyle(style)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build()

                    try {
                        NotificationManagerCompat.from(context).notify(notifIntId, updated)
                    } catch (_: SecurityException) {
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.bsdcampus.app.ACTION_NOTIFICATION_REPLY"
        const val ACTION_MARK_READ = "com.bsdcampus.app.ACTION_NOTIFICATION_MARK_READ"
        const val KEY_TEXT_REPLY = "bsd_key_text_reply"

        const val EXTRA_NOTIFICATION_INT_ID = "bsd_notif_int_id"
        const val EXTRA_RECIPIENT_ID = "bsd_notif_recipient_id"
        const val EXTRA_RECIPIENT_NAME = "bsd_notif_recipient_name"
        const val EXTRA_RECIPIENT_AVATAR = "bsd_notif_recipient_avatar"
        const val EXTRA_INCOMING_PREVIEW = "bsd_notif_incoming_preview"
        const val EXTRA_MESSAGE_DOC_ID = "bsd_notif_message_doc_id"
        const val EXTRA_ACTIVITY_DOC_ID = "bsd_notif_activity_doc_id"
    }
}
