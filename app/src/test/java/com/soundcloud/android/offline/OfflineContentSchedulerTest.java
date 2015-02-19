package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowPendingIntent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentSchedulerTest {

    private static long RETRY_TIME = 1000;

    private OfflineContentScheduler scheduler;

    @Mock private AlarmManager alarmManager;
    @Mock private ResumeDownloadOnConnectedReceiver resumeReceiver;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Captor private ArgumentCaptor<PendingIntent> intentArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        scheduler = new OfflineContentScheduler(Robolectric.application, alarmManager,
                resumeReceiver, networkConnectionHelper);
    }

    @Test
    public void schedulerRetryRegistersConnectionListenerToRetryIfNotConnected() throws Exception {
        scheduler.scheduleRetry();
        verify(resumeReceiver).register();
    }

    @Test
    public void schedulerRetryDoesNotRegistersConnectionListenerToRetryIfAlreadyConnected() throws Exception {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        scheduler.scheduleRetry();
        verify(resumeReceiver, never()).register();
    }

    @Test
    public void schedulerRetrySchedulesARetryAtDelay() throws Exception {
        scheduler.scheduleDelayedRetry(RETRY_TIME);
        verify(alarmManager).set(eq(OfflineContentScheduler.ALARM_TYPE), eq(RETRY_TIME), intentArgumentCaptor.capture());
        verifyPendingIntent();
    }

    @Test
    public void cancelPendingRetryCancelsThroughAlarmManager() throws Exception {
        scheduler.cancelPendingRetries();
        verify(alarmManager).cancel(intentArgumentCaptor.capture());
        verifyPendingIntent();
    }

    @Test
    public void cancelPendingRetryUnregistersConnectionListener() throws Exception {
        scheduler.cancelPendingRetries();
        verify(resumeReceiver).unregister();
    }

    private void verifyPendingIntent() {
        final ShadowPendingIntent shadowPendingIntent = Robolectric.shadowOf(intentArgumentCaptor.getValue());
        expect(shadowPendingIntent.getRequestCode()).toEqual(OfflineContentScheduler.REQUEST_ID);
        expect(shadowPendingIntent.getSavedIntent()).toEqual(new Intent(Robolectric.application, OfflineContentStartReceiver.class));
    }
}