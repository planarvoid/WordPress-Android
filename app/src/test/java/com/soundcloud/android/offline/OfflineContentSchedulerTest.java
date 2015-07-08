package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
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

public class OfflineContentSchedulerTest extends AndroidUnitTest {

    private final static long RETRY_TIME = 1000;

    private OfflineContentScheduler scheduler;

    @Mock private AlarmManager alarmManager;
    @Mock private ResumeDownloadOnConnectedReceiver resumeReceiver;
    @Mock private DownloadOperations downloadOperations;
    @Mock private Context context;
    @Captor private ArgumentCaptor<PendingIntent> intentArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        scheduler = new OfflineContentScheduler(context, alarmManager,
                resumeReceiver, downloadOperations);
    }

    @Test
    public void schedulerRetryRegistersConnectionListenerToRetryIfNotConnected() throws Exception {
        scheduler.scheduleRetry();
        verify(resumeReceiver).register();
    }

    @Test
    public void schedulerRetryDoesNotRegistersConnectionListenerToRetryIfAlreadyConnected() throws Exception {
        when(downloadOperations.isValidNetwork()).thenReturn(true);
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
        ShadowPendingIntent intent = Shadows.shadowOf(intentArgumentCaptor.getValue());
        assertThat(intent.getRequestCode()).isEqualTo(OfflineContentScheduler.REQUEST_ID);
        assertThat(intent.getSavedIntent().getComponent().getClassName())
                .isEqualTo(OfflineContentStartReceiver.class.getCanonicalName());
    }
}