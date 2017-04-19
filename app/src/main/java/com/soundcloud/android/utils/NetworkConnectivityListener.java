package com.soundcloud.android.utils;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import javax.inject.Inject;

/**
 * A wrapper for a broadcast receiver that provides network connectivity state to the event bus
 */
public class NetworkConnectivityListener {

    private static final String TAG = "NetworkListener";

    private final Context context;
    private final ConnectivityBroadcastReceiver receiver;
    private final ConnectionHelper connectionHelper;
    private final EventBus eventBus;

    @Inject
    public NetworkConnectivityListener(Context context,
                                       ConnectionHelper connectionHelper,
                                       EventBus eventBus) {
        this.context = context;
        this.connectionHelper = connectionHelper;
        this.eventBus = eventBus;
        receiver = new ConnectivityBroadcastReceiver();
    }

    public void startListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
    }

    public void stopListening() {
        context.unregisterReceiver(receiver);
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
            Log.d(TAG, "Connectivity change detected, current connection : " + currentConnectionType);
            eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, currentConnectionType);
        }
    }
}
