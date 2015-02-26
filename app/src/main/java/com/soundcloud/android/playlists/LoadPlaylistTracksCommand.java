package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

class LoadPlaylistTracksCommand extends Command<Urn, List<PropertySet>, LoadPlaylistTracksCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPlaylistTracksCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        final String fullSoundIdColumn = Table.Sounds + "." + TableColumns.Sounds._ID;
        return database.query(Query.from(Table.PlaylistTracks.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        TableColumns.Sounds.TITLE,
                        TableColumns.Sounds.USER_ID,
                        TableColumns.Users.USERNAME,
                        TableColumns.Sounds.DURATION,
                        TableColumns.Sounds.PLAYBACK_COUNT,
                        TableColumns.Sounds.LIKES_COUNT,
                        TableColumns.Sounds.SHARING,
                        TableColumns.TrackDownloads.REQUESTED_AT,
                        TableColumns.TrackDownloads.DOWNLOADED_AT,
                        TableColumns.TrackDownloads.UNAVAILABLE_AT,
                        field(Table.TrackDownloads + "." + TableColumns.TrackDownloads.REMOVED_AT).as(TableColumns.TrackDownloads.REMOVED_AT))
                .innerJoin(Table.Sounds.name(), TableColumns.PlaylistTracks.TRACK_ID, fullSoundIdColumn)
                .innerJoin(Table.Users.name(), Table.Sounds + "." + TableColumns.Sounds.USER_ID, Table.Users + "." + TableColumns.Users._ID)
                .leftJoin(Table.TrackDownloads.name(), fullSoundIdColumn, Table.TrackDownloads + "." + TableColumns.TrackDownloads._ID)
                .whereEq(Table.Sounds + "." + TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, input.getNumericId())
                .order(Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.POSITION, Query.ORDER_ASC)
                .whereNull(Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.REMOVED_AT)).toList(new PlaylistTrackItemMapper());
    }
}
