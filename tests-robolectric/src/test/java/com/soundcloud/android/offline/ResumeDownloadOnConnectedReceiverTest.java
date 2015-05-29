package com.soundcloud.android.offline;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

@RunWith(SoundCloudTestRunner.class)
public class ResumeDownloadOnConnectedReceiverTest {

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
        expect(intentFilterCaptor.getValue().getAction(0)).toEqual(ConnectivityManager.CONNECTIVITY_ACTION);
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
        receiver.onReceive(Robolectric.application, null);

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    @Test
    public void onReceiveStartsSyncServiceWhenValidNetwork() throws Exception {
        when(downloadOperations.isValidNetwork()).thenReturn(true);

        receiver.onReceive(Robolectric.application, null);

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual(OfflineContentService.ACTION_START);
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }

    @Test
    public void onReceiveDoesNotStartOfflineServiceWhenNetworkNotValid() throws Exception {
        when(downloadOperations.isValidNetwork()).thenReturn(false);

        receiver.onReceive(Robolectric.application, null);

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }
}