package com.soundcloud.android.offline;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class OfflineSyncStartReceiver extends BroadcastReceiver {

    @Inject OfflineContentOperations operations;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);
        Log.d(OfflineSyncService.TAG, "Offline Content Sync Start Receiver is awake..... starting service");
        OfflineSyncService.startSyncing(context);
    }
}
