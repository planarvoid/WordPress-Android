package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;

import javax.inject.Inject;

public class RecentlyPlayedMigration implements Migration {

    private final SyncStateStorage stateStorage;

    @Inject
    public RecentlyPlayedMigration(SyncStateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void applyMigration() {
        stateStorage.clear(Syncable.RECENTLY_PLAYED);
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 738;
    }
}
