package com.soundcloud.android.sync;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

// Purges all local state pertaining to meta-data syncing.
public class SyncCleanupAction {

    private final SyncStateManager stateManager;
    private final SharedPreferences streamPrefs;
    private final SharedPreferences activitiesPrefs;

    @Inject
    public SyncCleanupAction(
            SyncStateManager stateManager,
            // we can't inject the storage classes here, since they're provisioned from SyncModule
            // which is not visible to any code that's injected by FeaturesModule
            @Named(StorageModule.STREAM_SYNC) SharedPreferences streamPrefs,
            @Named(StorageModule.ACTIVITIES_SYNC) SharedPreferences activitiesPrefs) {
        this.stateManager = stateManager;
        this.streamPrefs = streamPrefs;
        this.activitiesPrefs = activitiesPrefs;
    }

    public void clear() {
        stateManager.clear();
        streamPrefs.edit().clear().apply();
        activitiesPrefs.edit().clear().apply();
    }
}
