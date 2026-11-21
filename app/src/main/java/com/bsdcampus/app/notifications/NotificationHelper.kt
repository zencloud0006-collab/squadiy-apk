package com.bsdcampus.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.bsdcampus.app.MainActivity
import com.bsdcampus.app.R
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {
    const val CHANNEL_CHAT_ID = "bsd_chat_notifications"
    const val CHANNEL_ACTIVITY_ID = "bsd_activity_notifications"

    const val EXTRA_OPEN_TAB = "bsd_open_tab"
    const val EXTRA_CHAT_USER_ID = "bsd_chat_user_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val chatChannel = NotificationChannel(
            CHANNEL_CHAT_ID,
            "Campus Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Direct messages and voice notes from Squadify students and faculty"
            enableVibration(true)
        }

        val activityChannel = NotificationChannel(
            CHANNEL_ACTIVITY_ID,
            "Campus Activity & Notices",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Post likes, comments, new followers, and pinned community updates"
            enableVibration(true)
        }

        manager.createNotificationChannel(chatChannel)
        manager.createNotificationChannel(activityChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildReplyAction(
        context: Context,
        notificationIntId: Int,
        recipientId: String,
        recipientName: String,
        recipientAvatar: String?,
        incomingPreview: String,
        messageDocId: String?,
        activityDocId: String?
    ): NotificationCompat.Action {
        val replyLabel = "Reply to ${recipientName.substringBefore(" ").ifBlank { "Member" }}..."
        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_REPLY
            putExtra(NotificationReplyReceiver.EXTRA_NOTIFICATION_INT_ID, notificationIntId)
            putExtra(NotificationReplyReceiver.EXTRA_RECIPIENT_ID, recipientId)
            putExtra(NotificationReplyReceiver.EXTRA_RECIPIENT_NAME, recipientName)
            if (!recipientAvatar.isNullOrBlank() && recipientAvatar.length <= 2048) {
                putExtra(NotificationReplyReceiver.EXTRA_RECIPIENT_AVATAR, recipientAvatar)
            }
            putExtra(NotificationReplyReceiver.EXTRA_INCOMING_PREVIEW, incomingPreview.take(240))
            putExtra(NotificationReplyReceiver.EXTRA_MESSAGE_DOC_ID, messageDocId)
            putExtra(NotificationReplyReceiver.EXTRA_ACTIVITY_DOC_ID, activityDocId)
        }
        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationIntId xor 0x41F2,
            replyIntent,
            replyFlags
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification_bsd,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }

    private fun buildMarkReadAction(
        context: Context,
        notificationIntId: Int,
        messageDocId: String?,
        activityDocId: String?
    ): NotificationCompat.Action {
        val markIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_MARK_READ
            putExtra(NotificationReplyReceiver.EXTRA_NOTIFICATION_INT_ID, notificationIntId)
            putExtra(NotificationReplyReceiver.EXTRA_MESSAGE_DOC_ID, messageDocId)
            putExtra(NotificationReplyReceiver.EXTRA_ACTIVITY_DOC_ID, activityDocId)
        }
        val markPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationIntId xor 0x79C1,
            markIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification_bsd,
            "Mark read",
            markPendingIntent
        ).build()
    }

    suspend fun showChatNotification(
        context: Context,
        messageId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String?,
        previewText: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notificationIntId = ("chat_thread_$senderId").hashCode()
        val avatarBitmap = loadCircularSenderBitmap(context, senderAvatar, senderName)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, "chat")
            putExtra(EXTRA_CHAT_USER_ID, senderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mePerson = Person.Builder()
            .setKey("me")
            .setName("You")
            .build()
        val senderPerson = Person.Builder()
            .setKey(senderId)
            .setName(senderName)
            .setIcon(IconCompat.createWithBitmap(avatarBitmap))
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
            .addMessage(previewText, System.currentTimeMillis(), senderPerson)

        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(R.drawable.ic_notification_bsd)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(senderName)
            .setContentText(previewText)
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                buildReplyAction(
                    context = context,
                    notificationIntId = notificationIntId,
                    recipientId = senderId,
                    recipientName = senderName,
                    recipientAvatar = senderAvatar,
                    incomingPreview = previewText,
                    messageDocId = messageId,
                    activityDocId = null
                )
            )
            .addAction(
                buildMarkReadAction(
                    context = context,
                    notificationIntId = notificationIntId,
                    messageDocId = messageId,
                    activityDocId = null
                )
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationIntId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    suspend fun showActivityNotification(
        context: Context,
        notificationId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String?,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notificationIntId = ("activity_$notificationId").hashCode()
        val displaySender = senderName.ifBlank { "Squadify" }
        val avatarBitmap = loadCircularSenderBitmap(context, senderAvatar, displaySender)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, "activity")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mePerson = Person.Builder()
            .setKey("me")
            .setName("You")
            .build()
        val senderPerson = Person.Builder()
            .setKey(senderId.ifBlank { displaySender })
            .setName(displaySender)
            .setIcon(IconCompat.createWithBitmap(avatarBitmap))
            .build()

        val bodyText = if (title.isNotBlank() && !message.startsWith(title)) {
            "$title • $message"
        } else {
            message
        }

        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
            .addMessage(bodyText, System.currentTimeMillis(), senderPerson)

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVITY_ID)
            .setSmallIcon(R.drawable.ic_notification_bsd)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(title.ifBlank { displaySender })
            .setContentText(message)
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (senderId.isNotBlank()) {
            builder.addAction(
                buildReplyAction(
                    context = context,
                    notificationIntId = notificationIntId,
                    recipientId = senderId,
                    recipientName = displaySender,
                    recipientAvatar = senderAvatar,
                    incomingPreview = bodyText,
                    messageDocId = null,
                    activityDocId = notificationId
                )
            )
        }
        builder.addAction(
            buildMarkReadAction(
                context = context,
                notificationIntId = notificationIntId,
                messageDocId = null,
                activityDocId = notificationId
            )
        )

        try {
            NotificationManagerCompat.from(context).notify(notificationIntId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showStoryUploadNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            "story_upload".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVITY_ID)
            .setSmallIcon(R.drawable.ic_notification_bsd)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        try {
            NotificationManagerCompat.from(context).notify(0x5708, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showMediaUploadNotification(
        context: Context,
        title: String,
        message: String,
        notificationIntId: Int = 0x5709
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIntId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVITY_ID)
            .setSmallIcon(R.drawable.ic_notification_bsd)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        try {
            NotificationManagerCompat.from(context).notify(notificationIntId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    suspend fun loadCircularSenderBitmap(
        context: Context,
        avatarUrl: String?,
        senderName: String
    ): Bitmap = withContext(Dispatchers.IO) {
        val targetPx = 128
        val rawBitmap: Bitmap? = try {
            when {
                avatarUrl.isNullOrBlank() -> null
                avatarUrl.startsWith("data:") -> {
                    val bytes = MediaUtils.decodeDataUriBytes(avatarUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                }
                MediaUtils.isChunkedMediaUri(avatarUrl) -> {
                    val bytes = CampusMediaRepository.resolveMediaImageBytes(context, avatarUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                }
                avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") -> {
                    // Skip SVG-only URLs quickly since BitmapFactory only decodes raster formats
                    if (avatarUrl.contains("/svg", ignoreCase = true)) {
                        null
                    } else {
                        val conn = (URL(avatarUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 3500
                            readTimeout = 3500
                            instanceFollowRedirects = true
                        }
                        try {
                            if (conn.responseCode in 200..299) {
                                conn.inputStream.use { BitmapFactory.decodeStream(it) }
                            } else null
                        } finally {
                            conn.disconnect()
                        }
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        if (rawBitmap != null) {
            toCircularBitmap(rawBitmap, targetPx).also {
                if (rawBitmap != it) rawBitmap.recycle()
            }
        } else {
            createMonogramAvatarBitmap(senderName, targetPx)
        }
    }

    private fun toCircularBitmap(source: Bitmap, sizePx: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, sizePx, sizePx)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcSize = minOf(source.width, source.height)
        val srcLeft = (source.width - srcSize) / 2
        val srcTop = (source.height - srcSize) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + srcSize, srcTop + srcSize)
        canvas.drawBitmap(source, srcRect, rect, paint)
        return output
    }

    private fun createMonogramAvatarBitmap(name: String, sizePx: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                intArrayOf(
                    Color.parseColor("#F58529"),
                    Color.parseColor("#DD2A7B"),
                    Color.parseColor("#8134AF")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        val initials = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "B" }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.40f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, radius, textY, textPaint)
        return output
    }
}
