package com.soundcloud.android.policies;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.AlarmManager;
import android.app.PendingIntent;

public class DailyUpdateSchedulerTest extends AndroidUnitTest {

    private DailyUpdateScheduler scheduler;
    private long currentTime = 1000L;

    @Mock private AlarmManager alarmManager;

    @Before
    public void setUp() throws Exception {
        scheduler = new DailyUpdateScheduler(context(),
                                             alarmManager,
                                             new TestDateProvider(currentTime));
    }

    @Test
    public void scheduleDailyPolicyUpdatesIfNotYetScheduled() {
        scheduler.schedule();
        scheduler.schedule();

        verify(alarmManager, times(1)).setInexactRepeating(eq(DailyUpdateScheduler.ALARM_TYPE),
                                                           eq(currentTime + DailyUpdateScheduler.POLICY_UPDATE_DELAY),
                                                           eq(AlarmManager.INTERVAL_DAY),
                                                           any(PendingIntent.class));
    }
}
