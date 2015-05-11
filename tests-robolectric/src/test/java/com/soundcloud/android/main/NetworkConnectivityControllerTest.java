package com.soundcloud.android.main;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

@RunWith(SoundCloudTestRunner.class)
public class NetworkConnectivityControllerTest {
    @Captor ArgumentCaptor<Handler> handlerArgumentCaptor;
    @Captor ArgumentCaptor<Integer> whatArgumentCaptor;
    @Captor ArgumentCaptor<Intent> intentArgumentCaptor;
    @Mock private AppCompatActivity activity;
    @Mock private NetworkInfo networkInfo;
    @Mock private NetworkConnectivityListener listener;
    @Mock private Context context;

    private NetworkConnectivityController lightCycle;

    @Before
    public void setUp() throws Exception {
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        lightCycle = new NetworkConnectivityController(context, listener);
    }

    @Test
    public void registerListenerOnCreate() {
        lightCycle.onCreate(activity, new Bundle());

        verify(listener).registerHandler(any(Handler.class), anyInt());
    }

    @Test
    public void startListeningOnStart() {
        lightCycle.onStart(activity);

        verify(listener).startListening(context);
    }

    @Test
    public void stopListeningOnStop() {
        lightCycle.onStop(activity);

        verify(listener).stopListening();
    }

    @Test
    public void isConnectedWhenNoListener() {
        lightCycle = new NetworkConnectivityController(context, null);

        expect(lightCycle.isConnected()).toBeTrue();
    }

    @Test
    public void returnNetworkInfoStatusWhenNoMessageDispatched() {
        when(listener.getNetworkInfo()).thenReturn(networkInfo);

        expect(lightCycle.isConnected()).toBeTrue();
    }

    @Test
    public void returnNetworkInfoStatusWhenMessageIsDispatched() {
        lightCycle.onCreate(activity, new Bundle());
        lightCycle.onStart(activity);
        verify(listener).registerHandler(handlerArgumentCaptor.capture(), whatArgumentCaptor.capture());

        final Handler handler = handlerArgumentCaptor.getValue();
        final Message message = handler.obtainMessage(whatArgumentCaptor.getValue(), networkInfo);
        handler.handleMessage(message);

        expect(lightCycle.isConnected()).toBeTrue();
    }

    @Test
    public void broadcastInfoWhenMessageIsReceived() {
        lightCycle.onCreate(activity, new Bundle());
        lightCycle.onStart(activity);
        verify(listener).registerHandler(handlerArgumentCaptor.capture(), whatArgumentCaptor.capture());

        final Handler handler = handlerArgumentCaptor.getValue();
        final Message message = handler.obtainMessage(whatArgumentCaptor.getValue(), networkInfo);
        handler.handleMessage(message);

        verify(context).sendBroadcast(intentArgumentCaptor.capture());
        final Intent intent = intentArgumentCaptor.getValue();
        expect(intent.getAction()).toEqual(Actions.CHANGE_PROXY_ACTION);
    }
}