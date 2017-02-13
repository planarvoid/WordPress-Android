package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class EntitySyncStateStorage {

    private final SharedPreferences preferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public EntitySyncStateStorage(@Named(StorageModule.ENTITY_SYNC_STATE) SharedPreferences preferences,
                                  CurrentDateProvider dateProvider) {
        this.preferences = preferences;
        this.dateProvider = dateProvider;
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    public void synced(Urn entity) {
        preferences.edit().putLong(entity.toString(), dateProvider.getCurrentTime()).apply();
    }

    boolean hasSyncedWithin(Urn entity, long timeInMs) {
        long threshold = dateProvider.getCurrentTime() - timeInMs;
        return lastSyncTime(entity) >= threshold;
    }

    public long lastSyncTime(Urn entity) {
        return preferences.getLong(entity.toString(), Consts.NOT_SET);
    }

    public boolean hasSyncedBefore(Urn entity) {
        return preferences.contains(entity.toString());
    }
}
