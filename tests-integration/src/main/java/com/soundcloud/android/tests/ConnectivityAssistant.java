package com.soundcloud.android.tests;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.test.ActivityInstrumentationTestCase2;

import java.lang.reflect.Method;

public class ConnectivityAssistant {

    private ActivityInstrumentationTestCase2 testCase;

    public ConnectivityAssistant(ActivityInstrumentationTestCase2 testCase){
        this.testCase = testCase;
    }

    public void turnWifiOn() {
        WifiManager wifiManager = getWifiManager();
        wifiManager.setWifiEnabled(true);
    }

    public void turnWifiOff() {

        Context context = testCase.getActivity().getApplicationContext();
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Method m = conman.getClass().getDeclaredMethod("setMobileDataEnabled", boolean.class);
            m.invoke(conman, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        WifiManager wifiManager = getWifiManager();
//        wifiManager.setWifiEnabled(false);
    }

    private WifiManager getWifiManager() {
        return (WifiManager) testCase.getInstrumentation()
                    .getTargetContext().getSystemService(Context.WIFI_SERVICE);
    }
}
