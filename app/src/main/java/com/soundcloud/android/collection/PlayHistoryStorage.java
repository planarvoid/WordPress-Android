package com.soundcloud.android.collection;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class PlayHistoryStorage {

    private final PropellerDatabase database;

    @Inject
    public PlayHistoryStorage(PropellerDatabase database) {
        this.database = database;
    }

    public void clear() {
        database.delete(Tables.PlayHistory.TABLE);
    }

}
