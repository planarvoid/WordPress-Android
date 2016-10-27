package com.soundcloud.android.comments;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class CommentsStorage {
    private final PropellerDatabase database;

    @Inject
    CommentsStorage(PropellerDatabase database) {
        this.database = database;
    }

    public void clear() {
        database.delete(Tables.Comments.TABLE);
    }
}
