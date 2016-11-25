package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineFilters;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistStorage {

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final AccountOperations accountOperations;

    @Inject
    public PlaylistStorage(PropellerDatabase propeller,
                           PropellerRx propellerRx,
                           AccountOperations accountOperations) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.accountOperations = accountOperations;
    }

    public boolean hasLocalChanges() {
        final QueryResult queryResult =
                propeller.query(apply(exists(from(Tables.Sounds.TABLE)
                        .select(TableColumns.SoundView._ID, Tables.Sounds.REMOVED_AT)
                        .whereEq(TableColumns.SoundView._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereLt(Tables.Sounds._ID, 0)).as("has_local_playlists")
                        .orWhereNotNull(Tables.Sounds.REMOVED_AT)));
        return queryResult.first(Boolean.class);
    }

    public Set<Urn> getPlaylistsDueForSync() {
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

    Observable<List<PlaylistItem>> loadPlaylists(Set<Urn> playlistUrns) {
        return propellerRx.query(buildPlaylistQuery(playlistUrns))
                          .map(new PlaylistInfoMapper(accountOperations.getLoggedInUserUrn()))
                          .map(new Func1<PropertySet, PlaylistItem>() {
                              @Override
                              public PlaylistItem call(PropertySet propertyBindings) {
                                  return PlaylistItem.from(propertyBindings);
                              }
                          })
                          .toList();
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

    public PropertySet loadPlaylistModifications(Urn playlistUrn) {
        return propeller.query(buildPlaylistModificationQuery(playlistUrn))
                        .firstOrDefault(new PlaylistModificationMapper(), PropertySet.create());
    }

    public Observable<PropertySet> loadPlaylist(Urn playlistUrn) {
        return propellerRx.query(buildPlaylistQuery(Sets.newHashSet(playlistUrn)))
                          .map(new PlaylistInfoMapper(accountOperations.getLoggedInUserUrn()))
                          .defaultIfEmpty(PropertySet.create());
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
        List<Long> playlistIds = Urns.toIds(new ArrayList<Urn>(urns));

        return  Query.from(Tables.PlaylistView.TABLE)
                .select(
                        Tables.PlaylistView.ID,
                        Tables.PlaylistView.TITLE,
                        Tables.PlaylistView.USERNAME,
                        Tables.PlaylistView.USER_ID,
                        Tables.PlaylistView.DURATION,
                        Tables.PlaylistView.TRACK_COUNT,
                        Tables.PlaylistView.LIKES_COUNT,
                        Tables.PlaylistView.REPOSTS_COUNT,
                        Tables.PlaylistView.ARTWORK_URL,
                        Tables.PlaylistView.PERMALINK_URL,
                        Tables.PlaylistView.SHARING,
                        Tables.PlaylistView.CREATED_AT,
                        Tables.PlaylistView.ARTWORK_URL,
                        Tables.PlaylistView.IS_ALBUM,
                        Tables.PlaylistView.SET_TYPE,
                        Tables.PlaylistView.RELEASE_DATE,
                        Tables.PlaylistView.LOCAL_TRACK_COUNT,
                        Tables.PlaylistView.IS_USER_LIKE,
                        Tables.PlaylistView.IS_USER_REPOST,
                        Tables.PlaylistView.HAS_PENDING_DOWNLOAD_REQUEST,
                        Tables.PlaylistView.HAS_DOWNLOADED_TRACKS,
                        Tables.PlaylistView.HAS_UNAVAILABLE_TRACKS,
                        Tables.PlaylistView.IS_MARKED_FOR_OFFLINE
                ).whereIn(Tables.PlaylistView.ID, playlistIds);
    }

    private Query hasOfflineTracks(List<Long> playlistIds) {
        return getQuery(playlistIds, OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER);
    }

    private Query hasUnavailableTracks(List<Long> playlistIds) {
        return getQuery(playlistIds, OfflineFilters.UNAVAILABLE_OFFLINE_TRACK_FILTER);
    }

    private Query pendingPlaylistTracksUrns(List<Long> playlistIds) {
        return getQuery(playlistIds, OfflineFilters.REQUESTED_DOWNLOAD_FILTER);
    }

    private Query getQuery(List<Long> playlistIds, Where offlineFilter) {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.SoundView._ID),
                         Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID))
                .whereEq(Table.SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_PLAYLIST);
        return Query
                .from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID.qualifiedName())
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
                .innerJoin(SoundView.name(), joinConditions)
                .whereIn(SoundView.field(TableColumns.SoundView._ID), playlistIds)
                .where(offlineFilter);
    }

    private static class PlaylistModificationMapper implements ResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(PlaylistProperty.URN, Urn.forPlaylist(cursorReader.getLong(Tables.Sounds._ID)));
            propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(Tables.Sounds.TITLE));
            propertySet.put(PlaylistProperty.IS_PRIVATE,
                            Sharing.PRIVATE.name()
                                           .equalsIgnoreCase(cursorReader.getString(Tables.Sounds.SHARING)));
            return propertySet;
        }
    }
}
