package com.soundcloud.android.tests.network;


import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;


/*
An example of how to use the new NetworkManager to control network connection status inside a test
 */
public class NetworkTest extends ActivityTest<MainActivity> {

    public NetworkTest() {
        super(MainActivity.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        // ensure wifi is switched on, since we always assume to have network connectivity at the
        // beginning of a test
        networkManagerClient.switchWifiOn();
    }

    public void ignore_testSwitchOffWifi() {
        networkManagerClient.switchWifiOff();
        assertFalse("Wifi was not switched off", networkManagerClient.isWifiEnabled());
    }

    public void ignore_testSwitchOnWifi() {
        networkManagerClient.switchWifiOff();
        assertFalse("Wifi was not switched off", networkManagerClient.isWifiEnabled());

        networkManagerClient.switchWifiOn();
        assertTrue("Wifi was not switched on", networkManagerClient.isWifiEnabled());
    }
}
