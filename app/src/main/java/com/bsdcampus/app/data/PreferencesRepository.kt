package com.bsdcampus.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bsd_campus_prefs")

enum class AppThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark (Pitch Black)");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

class PreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("bsd_theme_mode")
    private val autoCacheEnabledKey = booleanPreferencesKey("auto_cache_enabled")
    private val cacheMaxSizeMbKey = intPreferencesKey("cache_max_size_mb")
    private val cacheMaxAgeHoursKey = intPreferencesKey("cache_max_age_hours")
    private val lastAutoCacheCleanKey = longPreferencesKey("last_auto_cache_clean_ms")
    private val pendingDeletedPostIdsKey = stringSetPreferencesKey("pending_deleted_post_ids")

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        AppThemeMode.fromKey(prefs[themeModeKey])
    }

    val autoCacheEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoCacheEnabledKey] ?: true
    }

    val cacheMaxSizeMb: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[cacheMaxSizeMbKey] ?: 150
    }

    val cacheMaxAgeHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[cacheMaxAgeHoursKey] ?: 48
    }

    val lastAutoCacheCleanMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[lastAutoCacheCleanKey] ?: 0L
    }

    val pendingDeletedPostIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[pendingDeletedPostIdsKey].orEmpty()
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.key
        }
    }

    suspend fun setAutoCacheEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoCacheEnabledKey] = enabled
        }
    }

    suspend fun setCacheMaxSizeMb(maxMb: Int) {
        context.dataStore.edit { prefs ->
            prefs[cacheMaxSizeMbKey] = maxMb.coerceIn(50, 1000)
        }
    }

    suspend fun setCacheMaxAgeHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[cacheMaxAgeHoursKey] = hours.coerceIn(6, 720)
        }
    }

    suspend fun recordAutoCacheClean(timestampMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[lastAutoCacheCleanKey] = timestampMs
        }
    }

    suspend fun addPendingDeletedPostId(postId: String) {
        if (postId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[pendingDeletedPostIdsKey].orEmpty()
            prefs[pendingDeletedPostIdsKey] = current + postId
        }
    }

    suspend fun removePendingDeletedPostId(postId: String) {
        if (postId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[pendingDeletedPostIdsKey].orEmpty()
            if (postId in current) {
                prefs[pendingDeletedPostIdsKey] = current - postId
            }
        }
    }
}

