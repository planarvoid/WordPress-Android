package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLikesPendingAdditionCommand extends LegacyCommand<Integer, List<PropertySet>, LoadLikesPendingAdditionCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikesPendingAdditionCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, input)
                .order(CREATED_AT, DESC)
                .whereNotNull(TableColumns.Likes.ADDED_AT))
                .toList(new LikeMapper());
    }
}
