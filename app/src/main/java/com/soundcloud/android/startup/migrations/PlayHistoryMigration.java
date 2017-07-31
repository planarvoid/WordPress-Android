package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;

import javax.inject.Inject;

public class PlayHistoryMigration implements Migration {

    private final SyncStateStorage stateStorage;

    @Inject
    public PlayHistoryMigration(SyncStateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void applyMigration() {
        stateStorage.clear(Syncable.PLAY_HISTORY);
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 722;
    }
}
