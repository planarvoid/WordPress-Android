package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class SyncStateStorage {

    private final SharedPreferences preferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public SyncStateStorage(@Named(StorageModule.SYNCER) SharedPreferences preferences,
                            CurrentDateProvider dateProvider) {
        this.preferences = preferences;
        this.dateProvider = dateProvider;
    }

    public boolean hasSyncedBefore(Syncable syncable) {
        return hasSyncedBefore(syncable.name());
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    public void synced(Syncable syncable) {
        preferences.edit().putLong(syncable.name(), dateProvider.getCurrentTime()).apply();
    }

    public void synced(String entity) {
        preferences.edit().putLong(entity, dateProvider.getCurrentTime()).apply();
    }

    boolean hasSyncedWithin(Syncable syncable, long timeInMs) {
        return hasSyncedWithin(syncable.name(), timeInMs);
    }

    boolean hasSyncedWithin(String entity, long timeInMs) {
        long threshold = dateProvider.getCurrentTime() - timeInMs;
        return lastSyncTime(entity) >= threshold;
    }

    public long lastSyncTime(Syncable syncable) {
        return lastSyncTime(syncable.name());
    }

    private long lastSyncTime(String entity) {
        return preferences.getLong(entity, Consts.NOT_SET);
    }

    public boolean hasSyncedBefore(String entity) {
        return preferences.getLong(entity, Consts.NOT_SET) != Consts.NOT_SET;
    }

    void incrementSyncMisses(Syncable syncable) {
        preferences.edit().putInt(getMissesKey(syncable), getSyncMisses(syncable) + 1).apply();
    }

    void resetSyncMisses(Syncable syncable) {
        preferences.edit().putInt(getMissesKey(syncable), 0).apply();
    }

    int getSyncMisses(Syncable syncable) {
        return preferences.getInt(getMissesKey(syncable), 0);
    }

    private String getMissesKey(Syncable syncable) {
        return String.format("%s_misses", syncable.name());
    }
}
