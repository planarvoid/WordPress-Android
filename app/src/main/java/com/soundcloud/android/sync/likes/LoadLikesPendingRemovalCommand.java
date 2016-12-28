package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLikesPendingRemovalCommand extends Command<Integer, List<LikeRecord>> {

    private final PropellerDatabase database;

    @Inject
    LoadLikesPendingRemovalCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<LikeRecord> call(Integer soundType) {
        return database.query(Query.from(Tables.Likes.TABLE)
                                   .whereEq(Tables.Likes._TYPE, soundType)
                                   .order(Tables.Likes.CREATED_AT, DESC)
                                   .whereNotNull(Tables.Likes.REMOVED_AT))
                       .toList(new LikeMapper());
    }
}
