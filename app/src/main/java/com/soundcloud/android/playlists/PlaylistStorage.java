package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineFilters;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.HashSet;
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
                .where(hasLocalTracks())
                .where(isNotLocal()));

        Set<Urn> returnSet = new HashSet<>();
        for (CursorReader reader : queryResult) {
            returnSet.add(Urn.forPlaylist(reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID)));
        }
        return returnSet;
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

    public Observable<PropertySet> loadPlaylist(Urn playlistUrn) {
        return propellerRx.query(buildSinglePlaylistQuery(playlistUrn))
                .map(new PlaylistInfoMapper(accountOperations.getLoggedInUserUrn()))
                .defaultIfEmpty(PropertySet.create());
    }

    private Query buildSinglePlaylistQuery(Urn playlistUrn) {
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.USER_ID,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.REPOSTS_COUNT,
                        TableColumns.SoundView.PERMALINK_URL,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT,
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(likeQuery(playlistUrn)).as(TableColumns.SoundView.USER_LIKE),
                        exists(repostQuery(playlistUrn)).as(TableColumns.SoundView.USER_REPOST),
                        exists(pendingPlaylistTracksUrns(playlistUrn)).as(PostedPlaylistMapper.HAS_PENDING_DOWNLOAD_REQUEST),
                        exists(hasOfflineTracks(playlistUrn)).as(PostedPlaylistMapper.HAS_OFFLINE_TRACKS),
                        exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(OfflinePlaylistMapper.IS_MARKED_FOR_OFFLINE)
                        )
                .whereEq(TableColumns.SoundView._ID, playlistUrn.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .groupBy(Table.SoundView.field(TableColumns.SoundView._ID));
    }

    private Query hasOfflineTracks(Urn playlistUrn) {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.Sounds._ID), Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID))
                .whereEq(Table.SoundView.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST);
        return Query
                .from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID.qualifiedName())
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
                .innerJoin(SoundView.name(), joinConditions)
                .whereEq(SoundView.field(TableColumns.Sounds._ID), playlistUrn.getNumericId())
                .where(OfflineFilters.OFFLINE_TRACK_FILTER);
    }

    private Query pendingPlaylistTracksUrns(Urn playlistUrn) {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.Sounds._ID), Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID))
                .whereEq(Table.SoundView.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST);
        return Query
                .from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID.qualifiedName())
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
                .innerJoin(SoundView.name(), joinConditions)
                .whereEq(SoundView.field(TableColumns.Sounds._ID), playlistUrn.getNumericId())
                .where(OfflineFilters.REQUESTED_DOWNLOAD_FILTER);
    }

    private Query likeQuery(Urn playlistUrn) {
        final Where joinConditions = filter()
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), playlistUrn.getNumericId())
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }

    private Query repostQuery(Urn playlistUrn) {
        final Where joinConditions = filter()
                .whereEq(TableColumns.Sounds._ID, TableColumns.Posts.TARGET_ID)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Posts.TARGET_TYPE);

        return Query.from(Table.Posts.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereEq(TableColumns.Sounds._ID, playlistUrn.getNumericId())
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
    }

}
