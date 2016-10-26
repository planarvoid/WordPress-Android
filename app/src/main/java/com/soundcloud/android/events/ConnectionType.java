package com.soundcloud.android.events;

import com.soundcloud.android.utils.Log;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.util.EnumSet;

public enum ConnectionType {
    TWO_G("2g"),
    THREE_G("3g"),
    FOUR_G("4g"),
    WIFI("wifi"),
    OFFLINE("offline"),
    UNKNOWN("unknown");

    private static final EnumSet<ConnectionType> MOBILE =
            EnumSet.of(TWO_G, THREE_G, FOUR_G);


    private final String value;

    ConnectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConnectionType fromNetworkInfo(NetworkInfo activeNetworkInfo, int networkType) {
        if (activeNetworkInfo == null) {
            return ConnectionType.OFFLINE;
        }

        int activeNetworkType = activeNetworkInfo.getType();
        switch (activeNetworkType) {
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
                return ConnectionType.WIFI;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                return fromNetworkType(networkType);
            default:
                Log.d("ConnectionType", "No connection type match for Active Network type " + activeNetworkType);
                return ConnectionType.UNKNOWN;

        }
    }

    public Boolean isMobile() {
        return MOBILE.contains(this);
    }

    /**
     * Coarse classification is from
     * https://github.com/android/platform_frameworks_base/blob/master/telephony/java/android/telephony/TelephonyManager.java#L963
     */
    private static ConnectionType fromNetworkType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
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
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return ConnectionType.THREE_G;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return ConnectionType.FOUR_G;
            default:
                return ConnectionType.UNKNOWN;
        }
    }

}
