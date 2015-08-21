package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPendingIntent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class PolicyUpdateSchedulerTest extends AndroidUnitTest {

    private PolicyUpdateScheduler scheduler;
    private long currentTime = 1000L;

    @Mock private AlarmManager alarmManager;
    @Mock private Context context;
    @Mock private DateProvider dateProvider;
    @Mock private PolicyUpdateScheduler.PendingIntentFactory pendingIntentFactory;
    @Mock private PendingIntent intent;

    @Captor private ArgumentCaptor<PendingIntent> intentArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        scheduler = new PolicyUpdateScheduler(context, alarmManager, dateProvider, pendingIntentFactory);
    }

    @Test
    public void scheduleDailyPolicyUpdatesIfNotYetScheduled() {
        when(dateProvider.getCurrentTime()).thenReturn(currentTime);
        when(pendingIntentFactory.getPendingIntent(context, PendingIntent.FLAG_NO_CREATE)).thenReturn(null);
        when(pendingIntentFactory.getPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)).thenReturn(intent);

        scheduler.scheduleDailyPolicyUpdates();;

        verify(pendingIntentFactory).getPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
        verify(alarmManager).setInexactRepeating(PolicyUpdateScheduler.ALARM_TYPE, currentTime,
                PolicyUpdateScheduler.POLICY_UPDATE_DELAY, intent);
    }

    @Test
    public void scheduleDailyPolicyUpdatesDoesNothingIfAlreadyScheduled() {
        when(pendingIntentFactory.getPendingIntent(context, PendingIntent.FLAG_NO_CREATE)).thenReturn(intent);

        scheduler.scheduleDailyPolicyUpdates();

        verifyZeroInteractions(alarmManager);
    }
}