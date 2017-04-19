package com.soundcloud.android.utils;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

public class TestConnectionHelper implements ConnectionHelper {

    private final EventBus eventBus;
    private boolean isWifiConnected = true;
    private boolean isNetworkConnected = true;

    public TestConnectionHelper(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public ConnectionType getCurrentConnectionType() {
        if (isNetworkConnected) {
            if (isWifiConnected) {
                return ConnectionType.WIFI;
            } else {
                return ConnectionType.FOUR_G;
            }
        } else {
            return ConnectionType.OFFLINE;
        }
    }

    @Override
    public boolean isNetworkConnected() {
        return isNetworkConnected;
    }

    @Override
    public boolean isWifiConnected() {
        return isNetworkConnected() && isWifiConnected;
    }

    public void setWifiConnected(boolean wifiConnected) {
        isWifiConnected = wifiConnected;
        notifyNetworkChange();
    }

    public void setNetworkConnected(boolean networkConnected) {
        isNetworkConnected = networkConnected;
        notifyNetworkChange();
    }

    /**
     * Notifies like the broadcast receiver in {@link NetworkConnectivityListener} since it is not permitted
     * to send a {@link android.net.ConnectivityManager#CONNECTIVITY_ACTION} broadcast from within the app.
     */
    private void notifyNetworkChange() {
        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, getCurrentConnectionType());
    }
}
