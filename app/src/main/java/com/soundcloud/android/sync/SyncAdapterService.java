package com.soundcloud.android.sync;

import com.soundcloud.android.SoundCloudApplication;

import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Sync service - delegates to {@link ApiSyncService} for the actual syncing. This class is responsible for the setup
 * and handling of the background syncing.
 */
public class SyncAdapterService extends Service {

    private AbstractThreadedSyncAdapter syncAdapter;
    @Inject Provider<NewSyncAdapter> newSyncAdapterProvider;

    public SyncAdapterService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        syncAdapter = newSyncAdapterProvider.get();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
