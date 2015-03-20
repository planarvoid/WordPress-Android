package com.soundcloud.android.storage;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class PlaylistStorage {

    private final PropellerDatabase propeller;

    @Inject
    public PlaylistStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    public boolean hasLocalPlaylists() {
        final QueryResult queryResult = propeller.query(apply(exists(from(Table.Sounds.name())
                .select(TableColumns.SoundView._ID)
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(TableColumns.Sounds._ID, 0)).as("has_local_playlists")));
        return queryResult.first(Boolean.class);
    }

    public Set<Urn> getPlaylistsDueForSync() {
        final QueryResult queryResult = propeller.query(from(Table.PlaylistTracks.name())
                .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                .where(hasLocalTracks()));

        Set<Urn> returnSet = new HashSet<>();
        for (CursorReader reader : queryResult) {
            returnSet.add(Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID)));
        }
        return returnSet;
    }

    private Where hasLocalTracks() {
        return filter()
                .whereNotNull(TableColumns.PlaylistTracks.ADDED_AT)
                .orWhereNotNull(TableColumns.PlaylistTracks.REMOVED_AT);
    }
}
