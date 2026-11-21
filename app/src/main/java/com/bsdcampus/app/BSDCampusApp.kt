package com.bsdcampus.app

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.bsdcampus.app.data.CacheManager
import com.bsdcampus.app.data.PreferencesRepository
import com.bsdcampus.app.firebase.configureAppCheck
import com.bsdcampus.app.notifications.CampusNotificationCoordinator
import com.bsdcampus.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BSDCampusApp : Application() {
    private var startedActivities = 0
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        if (!Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            configureAppCheck(this, null)
            CampusNotificationCoordinator.init(this)
            scheduleAutoCacheCleanup()
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                CampusNotificationCoordinator.isAppInForeground = startedActivities > 0
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                CampusNotificationCoordinator.isAppInForeground = startedActivities > 0
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun scheduleAutoCacheCleanup() {
        appScope.launch {
            runCatching {
                val prefs = PreferencesRepository(applicationContext)
                val enabled = prefs.autoCacheEnabled.first()
                if (!enabled) return@launch
                val now = System.currentTimeMillis()
                val lastClean = prefs.lastAutoCacheCleanMs.first()
                // Run at most once every 4 hours on cold startup
                if (lastClean > 0L && now - lastClean < 4 * 60 * 60 * 1000L) {
                    return@launch
                }
                val maxSizeMb = prefs.cacheMaxSizeMb.first()
                val maxAgeHours = prefs.cacheMaxAgeHours.first()
                CacheManager.runAutoCleanup(
                    context = applicationContext,
                    maxSizeMb = maxSizeMb,
                    maxAgeHours = maxAgeHours
                )
                prefs.recordAutoCacheClean(now)
            }
        }
    }
}
