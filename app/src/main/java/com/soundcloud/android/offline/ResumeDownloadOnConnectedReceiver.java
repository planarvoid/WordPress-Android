package com.soundcloud.android.offline;

import com.soundcloud.android.utils.NetworkConnectionHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ResumeDownloadOnConnectedReceiver extends BroadcastReceiver {

    private final Context context;
    private final NetworkConnectionHelper connectionHelper;
    private boolean isRegistered;

    @Inject
    ResumeDownloadOnConnectedReceiver(Context context, NetworkConnectionHelper connectionHelper) {
        this.context = context;
        this.connectionHelper = connectionHelper;
    }

    void register() {
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(this, filter);
            isRegistered = true;
        }
    }

    void unregister() {
        if (isRegistered) {
            context.unregisterReceiver(this);
            isRegistered = false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (connectionHelper.networkIsConnected()) {
            OfflineContentService.start(context);
        }
    }
}
