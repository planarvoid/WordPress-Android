package com.soundcloud.android.storage;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class NewPlaylistStorage {

    private final PropellerDatabase propeller;

    @Inject
    public NewPlaylistStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    public boolean hasLocalPlaylists() {
        final QueryResult queryResult = propeller.query(Query.from(Table.Sounds.name())
                .select(TableColumns.SoundView._ID)
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(TableColumns.Sounds._ID, 0));
        return !queryResult.isEmpty();
    }

    public Set<Urn> getPlaylistsDueForSync() {
        final QueryResult queryResult = propeller.query(Query.from(Table.PlaylistTracks.name())
                .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                .where(getWhereHasLocalTracks()));

        Set<Urn> returnSet = new HashSet<>();
        for (CursorReader reader : queryResult) {
            returnSet.add(Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID)));
        }
        return returnSet;
    }

    private Where getWhereHasLocalTracks() {
        return new WhereBuilder().where(TableColumns.PlaylistTracks.ADDED_AT + " IS NOT NULL OR "
                + TableColumns.PlaylistTracks.REMOVED_AT + " IS NOT NULL");
    }

}
