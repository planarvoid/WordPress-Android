package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.utils.Urns.toIds;
import static com.soundcloud.android.utils.Urns.toIdsColl;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.playlists.LocalPlaylistChange;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistStorage {

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final NewPlaylistMapper playlistMapper;

    @Inject
    public PlaylistStorage(PropellerDatabase propeller,
                           PropellerRx propellerRx,
                           NewPlaylistMapper playlistMapper) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.playlistMapper = playlistMapper;
    }

    public boolean hasLocalChanges() {
        return hasLocalPlaylistChange() || hasLocalTrackChanges();
    }

    private Boolean hasLocalPlaylistChange() {
        return propeller.query(apply(exists(from(Tables.Sounds.TABLE)
                                                    .select(Tables.PlaylistView.ID, Tables.Sounds.REMOVED_AT)
                                                    .whereEq(_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                                                    .whereLt(Tables.Sounds._ID, 0)).as("has_local_playlists")
                                                                                   .orWhereNotNull(Tables.Sounds.REMOVED_AT))).first(Boolean.class);
    }

    private Boolean hasLocalTrackChanges() {
        return propeller.query(apply(exists(from(Table.PlaylistTracks.name())
                                                    .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                                                    .where(hasLocalTracks())
                                                    .where(isNotLocal())))).first(Boolean.class);
    }

    public Set<Urn> playlistWithTrackChanges() {
        final QueryResult queryResult = propeller.query(from(Table.PlaylistTracks.name())
                                                                .select(TableColumns.PlaylistTracks.PLAYLIST_ID)
                                                                .where(hasLocalTracks())
                                                                .where(isNotLocal()));

        Set<Urn> returnSet = new HashSet<>();
        for (CursorReader reader : queryResult) {
            returnSet.add(Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID)));
        }
        return returnSet;
    }

    Observable<List<Urn>> availablePlaylists(final Collection<Urn> playlistUrns) {
        return propellerRx
                .query(Query.from(Tables.PlaylistView.TABLE)
                            .select(Tables.PlaylistView.ID)
                            .whereIn(Tables.PlaylistView.ID, toIdsColl(playlistUrns)))
                .map(cursorReader -> Urn.forPlaylist(cursorReader.getLong(Tables.PlaylistView.ID)))
                .toList();
    }

    Observable<List<Playlist>> loadPlaylists(final Collection<Urn> playlistUrns) {
        return propellerRx.queryResult(buildPlaylistQuery(Sets.newHashSet(playlistUrns)))
                          .map(this::toPlaylistItems)
                          .firstOrDefault(Collections.emptyList());
    }

    private List<Playlist> toPlaylistItems(QueryResult cursorReaders) {
        final List<Playlist> playlists = new ArrayList<>(cursorReaders.getResultCount());

        for (CursorReader cursorReader : cursorReaders) {
            final Playlist playlist = playlistMapper.map(cursorReader);
            playlists.add(playlist);
        }
        return playlists;
    }

    private Where hasLocalTracks() {
        return filter()
                .whereNotNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.ADDED_AT))
                .orWhereNotNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));
    }

    private Where isNotLocal() {
        return filter()
                .whereGt(TableColumns.PlaylistTracks.PLAYLIST_ID, 0);
    }

    public Optional<LocalPlaylistChange> loadPlaylistModifications(Urn playlistUrn) {
        return Optional.fromNullable(propeller.query(buildPlaylistModificationQuery(playlistUrn)).firstOrDefault(new PlaylistModificationMapper(), null));
    }

    private Query buildPlaylistModificationQuery(Urn playlistUrn) {
        return Query.from(Tables.Sounds.TABLE)
                    .select(
                            Tables.Sounds._ID,
                            Tables.Sounds.TITLE,
                            Tables.Sounds.SHARING
                    )
                    .whereEq(Tables.Sounds._ID, playlistUrn.getNumericId())
                    .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                    .whereNotNull(Tables.Sounds.MODIFIED_AT);
    }

    private Query buildPlaylistQuery(Set<Urn> urns) {
        Collection<Long> playlistIds = toIds(new ArrayList<>(urns));

        return Query.from(Tables.PlaylistView.TABLE)
                    .select(Tables.PlaylistView.TABLE.name() + ".*"
                    ).whereIn(Tables.PlaylistView.ID, playlistIds);
    }

    private static class PlaylistModificationMapper implements ResultMapper<LocalPlaylistChange> {
        @Override
        public LocalPlaylistChange map(CursorReader cursorReader) {
            return LocalPlaylistChange.create(Urn.forPlaylist(cursorReader.getLong(Tables.Sounds._ID)),
                                              cursorReader.getString(Tables.Sounds.TITLE),
                                              Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(Tables.Sounds.SHARING)));
        }
    }
}
