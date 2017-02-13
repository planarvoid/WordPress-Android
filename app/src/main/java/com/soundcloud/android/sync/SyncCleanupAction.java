package com.soundcloud.android.sync;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

// Purges all local state pertaining to meta-data syncing.
public class SyncCleanupAction {

    private final SyncStateStorage syncStateStorage;
    private final EntitySyncStateStorage entitySyncStateStorage;
    private final SharedPreferences streamPrefs;
    private final SharedPreferences activitiesPrefs;

    @Inject
    public SyncCleanupAction(
            SyncStateStorage syncStateStorage,
            // we can't inject the storage classes here, since they're provisioned from SyncModule
            // which is not visible to any code that's injected by FeaturesModule
            EntitySyncStateStorage entitySyncStateStorage,
            @Named(StorageModule.STREAM_SYNC) SharedPreferences streamPrefs,
            @Named(StorageModule.ACTIVITIES_SYNC) SharedPreferences activitiesPrefs) {
        this.syncStateStorage = syncStateStorage;
        this.entitySyncStateStorage = entitySyncStateStorage;
        this.streamPrefs = streamPrefs;
        this.activitiesPrefs = activitiesPrefs;
    }

    public void clear() {
        entitySyncStateStorage.clear();
        syncStateStorage.clear();
        streamPrefs.edit().clear().apply();
        activitiesPrefs.edit().clear().apply();
    }
}
