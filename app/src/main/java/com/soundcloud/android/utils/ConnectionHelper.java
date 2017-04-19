package com.soundcloud.android.utils;

import com.soundcloud.android.events.ConnectionType;

public interface ConnectionHelper {
    ConnectionType getCurrentConnectionType();

    boolean isNetworkConnected();

    boolean isWifiConnected();
}
