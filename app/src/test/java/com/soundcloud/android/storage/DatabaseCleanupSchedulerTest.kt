package com.soundcloud.android.storage

import android.app.AlarmManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.TestDateProvider
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class DatabaseCleanupSchedulerTest : AndroidUnitTest() {

    private lateinit var scheduler: DatabaseCleanupScheduler
    private val currentTime = 1000L

    @Mock private lateinit var alarmManager: AlarmManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        scheduler = DatabaseCleanupScheduler(AndroidUnitTest.context(),
                alarmManager,
                TestDateProvider(currentTime))
    }

    @Test
    fun scheduleDailyPolicyUpdatesIfNotYetScheduled() {
        scheduler.schedule()
        scheduler.schedule()

        verify(alarmManager).setInexactRepeating(eq(DatabaseCleanupScheduler.ALARM_TYPE),
                eq(currentTime + DatabaseCleanupScheduler.DATABASE_CLEANUP_DELAY),
                eq(AlarmManager.INTERVAL_DAY),
                any())
    }
}
