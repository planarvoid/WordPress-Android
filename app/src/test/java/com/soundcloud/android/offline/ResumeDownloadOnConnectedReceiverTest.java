package com.soundcloud.android.offline;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class ResumeDownloadOnConnectedReceiverTest extends AndroidUnitTest {

    private ResumeDownloadOnConnectedReceiver receiver;

    @Mock private Context context;
    @Mock private DownloadOperations downloadOperations;
    @Captor private ArgumentCaptor<IntentFilter> intentFilterCaptor;

    @Before
    public void setUp() throws Exception {
        receiver = new ResumeDownloadOnConnectedReceiver(context, downloadOperations);
    }

    @Test
    public void registerWillRegisterIfNotAlreadyRegistered() throws Exception {
        receiver.register();

        verify(context).registerReceiver(same(receiver), intentFilterCaptor.capture());
        assertThat(intentFilterCaptor.getValue().getAction(0)).isEqualTo(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Test
    public void registerWillNotRegisterIfAlreadyRegistered() throws Exception {
        receiver.register();
        receiver.register();

        verify(context).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void registerWillNotUnregisterIfNotAlreadyRegistered() throws Exception {
        receiver.unregister();

        verify(context, never()).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void unregisterWillUnregisterIfAlreadyRegistered() throws Exception {
        receiver.register();
        receiver.unregister();

        verify(context).unregisterReceiver(receiver);
    }

    @Test
    public void onReceiveDoesNotStartSyncServiceIfNotConnected() throws Exception {
        receiver.onReceive(context, null);

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void onReceiveStartsSyncServiceWhenValidNetwork() throws Exception {
        when(downloadOperations.isValidNetwork()).thenReturn(true);

        receiver.onReceive(context, null);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void onReceiveDoesNotStartOfflineServiceWhenNetworkNotValid() throws Exception {
        when(downloadOperations.isValidNetwork()).thenReturn(false);

        receiver.onReceive(context, null);

        verify(context, never()).startService(any(Intent.class));
    }

    private Intent captureStartServiceIntent() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startService(captor.capture());

        return captor.getValue();
    }

    private boolean wasServiceStarted() {
        final Intent intent = captureStartServiceIntent();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_START) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }
}