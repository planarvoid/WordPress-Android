package com.soundcloud.android.utils;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowConnectivityManager;
import com.xtremelabs.robolectric.shadows.ShadowNetworkInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

@RunWith(DefaultTestRunner.class)
public class NetworkConnectivityListenerTest {

    NetworkConnectivityListener listener;
    ShadowConnectivityManager cm;

    @Before public void before() {
        listener = new NetworkConnectivityListener();
        listener.startListening(DefaultTestRunner.application);
        cm = Robolectric.shadowOf((ConnectivityManager)
                DefaultTestRunner.application.getSystemService(Context.CONNECTIVITY_SERVICE)
        );
    }

    @After public void after() {
        listener.stopListening();
    }

    @Test
    public void shouldListenenToConnectivity() throws Exception {
        TestHelper.simulateOffline();
        expect(listener.isConnected()).toBeFalse();
        TestHelper.simulateOnline();
        expect(listener.isConnected()).toBeTrue();
    }

    @Test
    public void shouldCheckWifiConnection() throws Exception {
        expect(listener.isWifiConnected()).toBeFalse();
        TestHelper.connectedViaWifi(true);
        expect(listener.isWifiConnected()).toBeTrue();
    }

    @Test
    public void shouldNullNetworkInfo() throws Exception {
        cm.setActiveNetworkInfo(null);
        expect(listener.isConnected()).toBeFalse();
        expect(listener.isWifiConnected()).toBeFalse();
    }

    @Test
    public void shouldReactToConnectivityBroadcasts() throws Exception {
        expect(listener.getState()).toBe(NetworkConnectivityListener.State.UNKNOWN);

        NetworkInfo info = ShadowNetworkInfo.newInstance(null);
        NetworkInfo other = ShadowNetworkInfo.newInstance(null);

        DefaultTestRunner.application.sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION)
            .putExtra(ConnectivityManager.EXTRA_NETWORK_INFO, info)
            .putExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO, other)
            .putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true));

        expect(listener.getState()).toBe(NetworkConnectivityListener.State.NOT_CONNECTED);
        expect(listener.getNetworkInfo()).toBe(info);
        expect(listener.getOtherNetworkInfo()).toBe(other);

        DefaultTestRunner.application.sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION)
                .putExtra(ConnectivityManager.EXTRA_NETWORK_INFO, info)
                .putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));

        expect(listener.getState()).toBe(NetworkConnectivityListener.State.CONNECTED);
    }

    @Test
    public void shouldRegisterHandler() throws Exception {
        final Object[] o = new Object[1];
        listener.registerHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                o[0] = msg.obj;
            }
        }, 0);

        final NetworkInfo info = ShadowNetworkInfo.newInstance();
        DefaultTestRunner.application.sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION)
                .putExtra(ConnectivityManager.EXTRA_NETWORK_INFO, info)
                .putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));

        expect(o[0]).toBe(info);
    }
}
