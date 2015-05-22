package com.soundcloud.android.likes;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.ColumnFunctions;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTrackUrnsCommand extends LegacyCommand<Object, List<Urn>, LoadLikedTrackUrnsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return database.query(Query.from(Table.Likes.name())
                .select(ColumnFunctions.field("Likes._id").as(BaseColumns._ID))
                .innerJoin("Sounds", "Likes._id", "Sounds._id")
                .whereEq("Likes." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order("Likes." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new TrackUrnMapper());
    }
}
