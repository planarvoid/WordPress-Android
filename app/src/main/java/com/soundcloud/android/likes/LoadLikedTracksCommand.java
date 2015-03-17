package com.soundcloud.android.likes;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTracksCommand extends LegacyCommand<ChronologicalQueryParams, List<PropertySet>, LoadLikedTracksCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTracksCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(buildQuery(input)).toList(new LikedTrackMapper());
    }

    protected Query buildQuery(ChronologicalQueryParams input) {
        return LoadLikedTrackCommand.trackLikeQuery()
                .whereLt(Table.Likes + "." + TableColumns.Likes.CREATED_AT, input.getTimestamp())
                .order(Table.Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .limit(input.getLimit());
    }
}
