package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

class ScSyncAdapter extends AbstractThreadedSyncAdapter {
    public ScSyncAdapter(SoundCloudApplication app) {
        super(app, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
            Log.d(SyncAdapterService.TAG, "onPerformSync("+account+","+extras+","+authority+","+provider+","+syncResult+")");
        }

        if (SyncConfig.shouldUpdateDashboard(getContext()) || SyncConfig.shouldSyncCollections(getContext())) {
            SyncAdapterService.performSync((SoundCloudApplication) getContext(), account, extras, provider, syncResult);
        } else {
            if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
                Log.d(SyncAdapterService.TAG, "skipping sync because Wifi is diabled");
            }
        }
        if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
            Log.d(SyncAdapterService.TAG,"Done with sync " + syncResult);
        }
    }
}
