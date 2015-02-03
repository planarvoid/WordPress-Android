package com.soundcloud.android.offline;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

@RunWith(SoundCloudTestRunner.class)
public class ResumeDownloadOnConnectedReceiverTest {

    private ResumeDownloadOnConnectedReceiver receiver;

    @Mock private Context context;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Captor private ArgumentCaptor<IntentFilter> intentFilterCaptor;

    @Before
    public void setUp() throws Exception {
        receiver = new ResumeDownloadOnConnectedReceiver(context, offlineContentOperations, networkConnectionHelper);
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
        receiver.onReceive(context, null);

        verify(offlineContentOperations, never()).startOfflineContentSyncing();
    }

    @Test
    public void onReceiveStartsSyncServiceIfConnected() throws Exception {
        // this test is weird, and does not test the right things, but we have to wait for refactors....
        final TestObservables.MockObservable objectObservable = TestObservables.emptyObservable();
        when(offlineContentOperations.updateOfflineLikes()).thenReturn(objectObservable);
        when(networkConnectionHelper.networkIsConnected()).thenReturn(true);

        receiver.onReceive(context, null);

        expect(objectObservable.subscribedTo()).toBeTrue();
    }
}