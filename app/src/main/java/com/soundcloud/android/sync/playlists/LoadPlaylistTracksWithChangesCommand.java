package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.List;

class LoadPlaylistTracksWithChangesCommand extends LegacyCommand<Urn, List<PropertySet>, LoadPlaylistTracksWithChangesCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPlaylistTracksWithChangesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(Query.from(Table.PlaylistTracks.name())
                .select(TableColumns.PlaylistTracks.TRACK_ID,
                        TableColumns.PlaylistTracks.ADDED_AT,
                        TableColumns.PlaylistTracks.REMOVED_AT)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, input.getNumericId())
                .order(POSITION, ASC))
                .toList(new PlaylistTrackUrnMapper());
    }

    private class PlaylistTrackUrnMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final Urn urn = Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID));
            final PropertySet playlistTrack = PropertySet.from(
                    PlaylistTrackProperty.TRACK_URN.bind(urn)
            );
            if (cursorReader.isNotNull(TableColumns.Likes.ADDED_AT)) {
                playlistTrack.put(PlaylistTrackProperty.ADDED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.ADDED_AT));
            }
            if (cursorReader.isNotNull(TableColumns.Likes.REMOVED_AT)) {
                playlistTrack.put(PlaylistTrackProperty.REMOVED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.REMOVED_AT));
            }
            return playlistTrack;
        }
    }

}
