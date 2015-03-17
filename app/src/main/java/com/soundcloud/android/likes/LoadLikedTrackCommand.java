package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTrackCommand extends LegacyCommand<Urn, PropertySet, LoadLikedTrackCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTrackCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final List<PropertySet> queryResult = database.query(buildQuery(input)).toList(new LikedTrackMapper());
        return queryResult.isEmpty() ? PropertySet.create() : queryResult.get(0);
    }

    private Query buildQuery(Urn input) {
        return trackLikeQuery().whereEq(Table.Likes + "." + TableColumns.Likes._ID, input.getNumericId());
    }

    static Query trackLikeQuery() {
        final String fullSoundIdColumn = Table.Sounds + "." + TableColumns.Sounds._ID;
        return Query.from(Table.Likes.name(), Table.Sounds.name(), Table.Users.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        TableColumns.Sounds.TITLE,
                        TableColumns.Users.USERNAME,
                        TableColumns.Sounds.DURATION,
                        TableColumns.Sounds.PLAYBACK_COUNT,
                        TableColumns.Sounds.LIKES_COUNT,
                        TableColumns.Sounds.SHARING,
                        TableColumns.TrackDownloads.REQUESTED_AT,
                        TableColumns.TrackDownloads.DOWNLOADED_AT,
                        TableColumns.TrackDownloads.UNAVAILABLE_AT,
                        field(Table.TrackDownloads + "." + TableColumns.TrackDownloads.REMOVED_AT).as(TableColumns.TrackDownloads.REMOVED_AT),
                        field(Table.Likes + "." + TableColumns.Likes.CREATED_AT).as(TableColumns.Likes.CREATED_AT))
                .leftJoin(Table.TrackDownloads.name(), fullSoundIdColumn, Table.TrackDownloads + "." + TableColumns.TrackDownloads._ID)
                .joinOn(Table.Likes + "." + TableColumns.Likes._ID, fullSoundIdColumn)
                .joinOn(Table.Sounds + "." + TableColumns.Sounds.USER_ID, Table.Users + "." + TableColumns.Users._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.Likes + "." + TableColumns.Likes.REMOVED_AT);
    }

}
