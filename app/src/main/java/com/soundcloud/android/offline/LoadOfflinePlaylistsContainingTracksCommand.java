package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

class LoadOfflinePlaylistsContainingTracksCommand extends Command<Collection<Urn>, List<Urn>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadOfflinePlaylistsContainingTracksCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<Urn> call(Collection<Urn> input) {
        return loadOfflinePlaylistsContainingTrack(input);
    }

    private List<Urn> loadOfflinePlaylistsContainingTrack(final Collection<Urn> tracks) {
        final Where joinConditions = filter()
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, Tables.OfflineContent._ID)
                .whereEq(Tables.OfflineContent._TYPE, Tables.OfflineContent.TYPE_PLAYLIST);

        final Query playlistsQuery = Query.from(Table.PlaylistTracks.name())
                                          .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                                          .innerJoin(Tables.OfflineContent.TABLE, joinConditions)
                                          .whereIn(TableColumns.PlaylistTracks.TRACK_ID, transform(tracks, Urns.TO_ID));

        return propeller
                .query(playlistsQuery)
                .toList(new PlaylistTracksUrnMapper());
    }

    private static class PlaylistTracksUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID));
        }
    }

}
