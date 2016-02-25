package com.soundcloud.android.offline;

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
    private final DownloadConnectionHelper downloadConnectionHelper;
    private boolean isRegistered;

    @Inject
    ResumeDownloadOnConnectedReceiver(Context context, DownloadConnectionHelper downloadConnectionHelper) {
        this.context = context;
        this.downloadConnectionHelper = downloadConnectionHelper;
    }

    void register() {
        if (!isRegistered) {
            final IntentFilter filter = new IntentFilter();
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
        if (downloadConnectionHelper.isDownloadPermitted()) {
            OfflineContentService.start(context);
        }
    }
}
