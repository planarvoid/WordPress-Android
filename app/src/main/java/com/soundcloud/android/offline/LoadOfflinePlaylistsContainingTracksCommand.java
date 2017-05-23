package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class LoadOfflinePlaylistsContainingTracksCommand extends Command<Collection<Urn>, List<Urn>> {
    private static final int DEFAULT_BATCH_SIZE = 500;

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

        List<Urn> result = new ArrayList<>(tracks.size());
        for (List<Urn> batch : Lists.partition(newArrayList(tracks), DEFAULT_BATCH_SIZE)) {
            result.addAll(propeller.query(Query.from(Table.PlaylistTracks.name())
                                               .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                                               .innerJoin(Tables.OfflineContent.TABLE, joinConditions)
                                               .whereIn(TableColumns.PlaylistTracks.TRACK_ID, transform(batch, Urns.TO_ID)))
                                   .toList(new PlaylistTracksUrnMapper()));
        }

        return result;
    }

    private static class PlaylistTracksUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID));
        }
    }

}
