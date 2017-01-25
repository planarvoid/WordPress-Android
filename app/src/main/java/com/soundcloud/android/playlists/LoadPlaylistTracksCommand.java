package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackStorage;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistTracksCommand extends Command<Urn, List<Track>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadPlaylistTracksCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<Track> call(Urn playlistUrn) {
        return propeller
                .query(getPlaylistTracksQuery(playlistUrn))
                .toList(TrackStorage::trackFromCursorReader);
    }

    private Query getPlaylistTracksQuery(Urn playlistUrn) {
        return Query.from(Table.PlaylistTracks.name())
                    .select("TrackView.*")
                    .innerJoin(Tables.TrackView.TABLE.name(),
                               Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID),
                               Tables.TrackView.ID.qualifiedName())
                    .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID),
                             playlistUrn.getNumericId())
                    .order(Table.PlaylistTracks.field(POSITION), ASC)
                    .whereNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));
    }
}
