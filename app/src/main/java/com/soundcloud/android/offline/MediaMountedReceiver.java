package com.soundcloud.android.offline;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class MediaMountedReceiver extends BroadcastReceiver {

    @Inject OfflineStorageOperations offlineStorageOperations;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
            SoundCloudApplication.getObjectGraph().inject(this);
            offlineStorageOperations.checkForOfflineStorageConsistency(context);
            offlineStorageOperations.updateOfflineContentOnSdCard();
        }
    }
}
