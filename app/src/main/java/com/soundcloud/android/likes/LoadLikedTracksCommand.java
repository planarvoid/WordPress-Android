package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;

public class LoadLikedTracksCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    @Inject
    LoadLikedTracksCommand(PropellerDatabase database) {
        super(database, new LikedTrackMapper());
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
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
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereLt(Table.Likes + "." + TableColumns.Likes.CREATED_AT, input.getTimestamp())
                .joinOn(Table.Likes + "." + TableColumns.Likes._ID, fullSoundIdColumn)
                .joinOn(Table.Sounds + "." + TableColumns.Sounds.USER_ID, Table.Users + "." + TableColumns.Users._ID)
                .order(Table.Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(Table.Likes + "." + TableColumns.Likes.REMOVED_AT);
    }
}
