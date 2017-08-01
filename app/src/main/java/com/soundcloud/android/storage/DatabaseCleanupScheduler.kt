package com.soundcloud.android.storage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.soundcloud.android.navigation.PendingIntentFactory
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DatabaseCleanupScheduler
@Inject
internal constructor(private val context: Context,
                     private val alarmManager: AlarmManager,
                     private val dateProvider: CurrentDateProvider) {

    fun schedule() {
        if (!isNextUpdateAlreadyScheduled()) {
            Log.d(DatabaseCleanupService.TAG, "Scheduling new policy update")
            val intent = PendingIntentFactory.createCleanupSchedulerIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
            val initialDelay = dateProvider.currentTime + DATABASE_CLEANUP_DELAY
            alarmManager.setInexactRepeating(ALARM_TYPE, initialDelay, AlarmManager.INTERVAL_DAY, intent)
        }
    }

    private fun isNextUpdateAlreadyScheduled() = PendingIntentFactory.createCleanupSchedulerIntent(context, PendingIntent.FLAG_NO_CREATE) != null

    companion object {
        const val ALARM_TYPE = AlarmManager.RTC_WAKEUP
        internal val DATABASE_CLEANUP_DELAY = TimeUnit.DAYS.toMillis(1)
    }
}
