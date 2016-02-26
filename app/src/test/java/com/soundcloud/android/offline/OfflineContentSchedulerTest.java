package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
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

    private OfflineContentScheduler scheduler;

    @Mock private AlarmManager alarmManager;
    @Mock private ResumeDownloadOnConnectedReceiver resumeReceiver;
    @Mock private DownloadConnectionHelper downloadConnectionHelper;
    @Mock private Context context;
    @Captor private ArgumentCaptor<PendingIntent> intentArgumentCaptor;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() {
        dateProvider = new TestDateProvider();
        scheduler = new OfflineContentScheduler(context, alarmManager, resumeReceiver, downloadConnectionHelper, dateProvider);
    }

    @Test
    public void scheduleRetryRegisterConnectionListenerWhenNonValidNetwork() {
        scheduler.scheduleRetryForConnectivityError();
        verify(resumeReceiver).register();
    }

    @Test
    public void scheduleRetryRegisterShouldRetryWhenValidNetwork() {
        when(downloadConnectionHelper.isDownloadPermitted()).thenReturn(true);
        scheduler.scheduleRetryForConnectivityError();
        verify(alarmManager).set(eq(OfflineContentScheduler.ALARM_TYPE), eq(dateProvider.getCurrentTime() + OfflineConstants.RETRY_DELAY), intentArgumentCaptor.capture());
        verifyPendingIntent();
    }

    @Test
    public void schedulerRetryDoesNotRegistersConnectionListenerToRetryIfAlreadyConnected() {
        when(downloadConnectionHelper.isDownloadPermitted()).thenReturn(true);
        scheduler.scheduleRetryForConnectivityError();
        verify(resumeReceiver, never()).register();
    }

    @Test
    public void scheduleCleanupAction() {
        scheduler.scheduleCleanupAction().call(null);

        verify(alarmManager).set(eq(OfflineContentScheduler.ALARM_TYPE), eq(dateProvider.getCurrentTime() + OfflineConstants.PENDING_REMOVAL_DELAY), intentArgumentCaptor.capture());
    }

    @Test
    public void cancelPendingRetryCancelsThroughAlarmManager() {
        scheduler.cancelPendingRetries();
        verify(alarmManager).cancel(intentArgumentCaptor.capture());
        verifyPendingIntent();
    }

    @Test
    public void cancelPendingRetryUnregistersConnectionListener() {
        scheduler.cancelPendingRetries();
        verify(resumeReceiver).unregister();
    }

    private void verifyPendingIntent() {
        ShadowPendingIntent intent = Shadows.shadowOf(intentArgumentCaptor.getValue());
        assertThat(intent.getRequestCode()).isEqualTo(OfflineContentScheduler.RETRY_REQUEST_ID);
        assertThat(intent.getSavedIntent().getComponent().getClassName()).isEqualTo(AlarmManagerReceiver.class.getCanonicalName());
        assertThat(intent.getSavedIntent().getAction()).isEqualTo(OfflineContentService.ACTION_START);
    }
}
