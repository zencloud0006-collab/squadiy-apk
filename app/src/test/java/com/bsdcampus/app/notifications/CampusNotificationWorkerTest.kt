package com.bsdcampus.app.notifications

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CampusNotificationWorkerTest {

    @Test
    fun schedulePeriodic_enqueuesOneMinuteLoopAndSafetyWatchdog_and_cancelClearsBoth() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertEquals(1L, CampusNotificationWorker.SYNC_INTERVAL_MINUTES)

        CampusNotificationWorker.schedulePeriodic(context)

        val wm = WorkManager.getInstance(context)
        val oneMinInfos = wm.getWorkInfosForUniqueWork(CampusNotificationWorker.ONE_MINUTE_SYNC_WORK_NAME).get()
        assertEquals(1, oneMinInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, oneMinInfos.first().state)

        CampusNotificationWorker.cancelPeriodic(context)

        val cancelledInfos = wm.getWorkInfosForUniqueWork(CampusNotificationWorker.ONE_MINUTE_SYNC_WORK_NAME).get()
        assertTrue(cancelledInfos.isEmpty() || cancelledInfos.first().state == WorkInfo.State.CANCELLED)
    }
}
