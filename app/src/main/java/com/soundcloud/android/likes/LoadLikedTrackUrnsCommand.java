package com.soundcloud.android.likes;

import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.ColumnFunctions;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

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
        final Where whereTrackDataExists = filter()
                .whereEq(Table.Likes.field(TableColumns.Likes._ID), Table.Sounds.field(TableColumns.Sounds._ID))
                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), Table.Sounds.field(TableColumns.Sounds._TYPE));

        return database.query(Query.from(Table.Likes.name())
                .select(ColumnFunctions.field("Likes._id").as(BaseColumns._ID))
                .innerJoin(Table.Sounds.name(), whereTrackDataExists)
                .whereEq("Likes." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order("Likes." + CREATED_AT, DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new TrackUrnMapper());
    }
}
