package com.soundcloud.android.utils;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class NetworkConnectivityListenerTest {

    NetworkConnectivityListener listener;

    @Before public void before() {
        listener = new NetworkConnectivityListener();
        listener.startListening(DefaultTestRunner.application);
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
}
