package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLikesPendingRemovalCommand extends Command<Integer, List<PropertySet>, LoadLikesPendingRemovalCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikesPendingRemovalCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, input)
                .order(TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNotNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikeMapper());
    }
}
