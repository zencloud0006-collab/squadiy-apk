package com.bsdcampus.app.notifications

import android.content.Context

object CampusNotificationStore {
    private const val PREFS_NAME = "bsd_notif_seen_prefs"
    private const val MAX_STORED_IDS = 350

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun isMessagesSeeded(context: Context, uid: String): Boolean =
        prefs(context).getBoolean("seeded_msgs_$uid", false)

    @Synchronized
    fun seedMessages(context: Context, uid: String, ids: Collection<String>) {
        val trimmed = ids.toList().takeLast(MAX_STORED_IDS).toSet()
        prefs(context).edit()
            .putBoolean("seeded_msgs_$uid", true)
            .putStringSet("seen_msg_ids_$uid", trimmed)
            .apply()
    }

    @Synchronized
    fun consumeUnseenMessageIds(context: Context, uid: String, candidateIds: List<String>): List<String> {
        val p = prefs(context)
        if (!p.getBoolean("seeded_msgs_$uid", false)) {
            seedMessages(context, uid, candidateIds)
            return emptyList()
        }
        val existing = p.getStringSet("seen_msg_ids_$uid", emptySet()).orEmpty().toMutableList()
        val existingSet = existing.toHashSet()
        val unseen = candidateIds.filter { it.isNotBlank() && it !in existingSet }
        if (unseen.isNotEmpty()) {
            existing.addAll(unseen)
            val trimmed = if (existing.size > MAX_STORED_IDS) {
                existing.takeLast(MAX_STORED_IDS).toSet()
            } else {
                existing.toSet()
            }
            p.edit().putStringSet("seen_msg_ids_$uid", trimmed).apply()
        }
        return unseen
    }

    @Synchronized
    fun isActivitySeeded(context: Context, uid: String): Boolean =
        prefs(context).getBoolean("seeded_notifs_$uid", false)

    @Synchronized
    fun seedActivity(context: Context, uid: String, ids: Collection<String>) {
        val trimmed = ids.toList().takeLast(MAX_STORED_IDS).toSet()
        prefs(context).edit()
            .putBoolean("seeded_notifs_$uid", true)
            .putStringSet("seen_notif_ids_$uid", trimmed)
            .apply()
    }

    @Synchronized
    fun consumeUnseenActivityIds(context: Context, uid: String, candidateIds: List<String>): List<String> {
        val p = prefs(context)
        if (!p.getBoolean("seeded_notifs_$uid", false)) {
            seedActivity(context, uid, candidateIds)
            return emptyList()
        }
        val existing = p.getStringSet("seen_notif_ids_$uid", emptySet()).orEmpty().toMutableList()
        val existingSet = existing.toHashSet()
        val unseen = candidateIds.filter { it.isNotBlank() && it !in existingSet }
        if (unseen.isNotEmpty()) {
            existing.addAll(unseen)
            val trimmed = if (existing.size > MAX_STORED_IDS) {
                existing.takeLast(MAX_STORED_IDS).toSet()
            } else {
                existing.toSet()
            }
            p.edit().putStringSet("seen_notif_ids_$uid", trimmed).apply()
        }
        return unseen
    }
}
