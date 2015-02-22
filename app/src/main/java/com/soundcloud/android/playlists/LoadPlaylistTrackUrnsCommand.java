package com.soundcloud.android.playlists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistTrackUrnsCommand extends Command<Urn, List<Urn>, LoadPlaylistTrackUrnsCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadPlaylistTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        Query query = Query.from(Table.PlaylistTracks.name())
                .select(TableColumns.PlaylistTracks.TRACK_ID)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, input.getNumericId())
                .order(TableColumns.PlaylistTracks.POSITION, Query.ORDER_ASC);
        return database.query(query).toList(new TrackUrnMapper());
    }

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID));
        }
    }
}
