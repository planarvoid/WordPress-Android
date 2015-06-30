package com.soundcloud.android.offline;

import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OfflineContentStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(OfflineContentService.TAG, "Offline Content Start Receiver notified. Starting service.");
        OfflineContentService.start(context);
    }
}
