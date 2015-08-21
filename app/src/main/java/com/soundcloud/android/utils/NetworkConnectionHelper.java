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
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if(activeNetworkInfo == null){
            return ConnectionType.OFFLINE;
        }

        int activeNetworkType = activeNetworkInfo.getType();
        switch (activeNetworkType){
            case ConnectivityManager.TYPE_WIFI :
            case ConnectivityManager.TYPE_WIMAX :
                return ConnectionType.WIFI;
            case ConnectivityManager.TYPE_MOBILE :
            case ConnectivityManager.TYPE_MOBILE_DUN :
            case ConnectivityManager.TYPE_MOBILE_HIPRI :
            case ConnectivityManager.TYPE_MOBILE_MMS :
            case ConnectivityManager.TYPE_MOBILE_SUPL :
                return fromNetworkType(telephonyManager.getNetworkType());
            default:
                Log.d(TAG, "No connection type match for Active Network type " + activeNetworkType);
                return ConnectionType.UNKNOWN;

        }
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

    public String getNetworkOperatorName() {
        return telephonyManager.getNetworkOperatorName();
    }

    /**
     * Coarse classification is from
     * https://github.com/android/platform_frameworks_base/blob/master/telephony/java/android/telephony/TelephonyManager.java#L963
     */
    private ConnectionType fromNetworkType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return ConnectionType.TWO_G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return ConnectionType.THREE_G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return ConnectionType.FOUR_G;
            default:
                return ConnectionType.UNKNOWN;
        }
    }
}


