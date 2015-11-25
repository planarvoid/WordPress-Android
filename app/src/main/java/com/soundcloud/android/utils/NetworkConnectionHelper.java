package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ConnectionType;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import javax.inject.Inject;

public class NetworkConnectionHelper {

    public static final String TAG = "NetworkConnectionHelper";

    private final ConnectivityManager connectivityManager;
    private final TelephonyManager telephonyManager;

    @Inject
    public NetworkConnectionHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager) {
        this.connectivityManager = connectivityManager;
        this.telephonyManager = telephonyManager;
    }

    @Deprecated
    public NetworkConnectionHelper(){
        this((ConnectivityManager)SoundCloudApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager)SoundCloudApplication.instance.getSystemService(Context.TELEPHONY_SERVICE));
    }

    public ConnectionType getCurrentConnectionType(){
        return ConnectionType.fromNetworkInfo(connectivityManager.getActiveNetworkInfo(), telephonyManager.getNetworkType());
    }

    public boolean isNetworkConnected(){
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public boolean isWifiConnected() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null
                && info.isConnected()
                && (info.getType() == ConnectivityManager.TYPE_WIFI
                || info.getType() == ConnectivityManager.TYPE_WIMAX);
    }
}


