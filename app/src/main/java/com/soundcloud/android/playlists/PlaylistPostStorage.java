package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistQueries.HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER;
import static com.soundcloud.android.playlists.PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY;
import static com.soundcloud.android.playlists.PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Posts;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.TableColumns.SoundView.CREATED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class PlaylistPostStorage {

    private final PropellerRx propellerRx;
    private final CurrentDateProvider dateProvider;

    @Inject
    public PlaylistPostStorage(PropellerRx propellerRx, CurrentDateProvider dateProvider) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    public Observable<List<PropertySet>> loadPostedPlaylists(int limit, long fromTimestamp) {
        return propellerRx.query(buildLoadPostedPlaylistsQuery(limit, fromTimestamp)).map(new PostedPlaylistMapper()).toList();
    }

    protected Query buildLoadPostedPlaylistsQuery(int limit, long fromTimestamp) {
        return Query.from(SoundView.name())
                .select(
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(SoundView.field(TableColumns.SoundView.USER_ID)).as(TableColumns.SoundView.USER_ID),
                        field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(Posts.field(TableColumns.Posts.CREATED_AT)).as(TableColumns.Posts.CREATED_AT),
                        count(PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(likeQuery()).as(TableColumns.SoundView.USER_LIKE),
                        exists(HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(PostedPlaylistMapper.HAS_PENDING_DOWNLOAD_REQUEST),
                        exists(HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER).as(PostedPlaylistMapper.HAS_OFFLINE_TRACKS),
                        exists(IS_MARKED_FOR_OFFLINE_QUERY).as(PostedPlaylistMapper.IS_MARKED_FOR_OFFLINE))
                .leftJoin(Table.PlaylistTracks.name(), SoundView.field(TableColumns.SoundView._ID), PLAYLIST_ID)
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), SoundView.field(TableColumns.SoundView._TYPE)))
                .leftJoin(TrackDownloads.TABLE.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(Posts.field(TableColumns.Posts.CREATED_AT), fromTimestamp)
                .groupBy(SoundView.field(TableColumns.SoundView._ID))
                .order(CREATED_AT, DESC)
                .limit(limit);
    }

    Observable<ChangeResult> markPendingRemoval(Urn urn) {
        return propellerRx.update(
                Sounds.name(),
                ContentValuesBuilder.values(1).put(TableColumns.Sounds.REMOVED_AT, dateProvider.getCurrentTime()).get(),
                filter()
                        .whereEq(TableColumns.Sounds._ID, urn.getNumericId())
                        .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
        );
    }

    Observable<ChangeResult> remove(Urn urn) {
        return propellerRx.delete(
                Sounds.name(),
                filter()
                        .whereEq(TableColumns.Sounds._ID, urn.getNumericId())
                        .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
        );
    }

    static Query likeQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                .whereEq(Table.SoundView.field(TableColumns.Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                // do not use SoundView here. The exists query will fail, in spite of passing tests
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }
}
