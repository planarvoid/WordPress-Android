package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY;
import static com.soundcloud.android.playlists.PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.TableColumns.SoundView.CREATED_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;

public class PlaylistPostStorage {

    private final PropellerRx propellerRx;

    @Inject
    public PlaylistPostStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<java.util.List<PropertySet>> loadPostedPlaylists(int limit, long fromTimestamp){
        return propellerRx.query(buildQuery(limit, fromTimestamp)).map(new PostedPlaylistMapper()).toList();
    }

    protected Query buildQuery(int limit, long fromTimestamp) {
        return Query.from(SoundView.name())
                .select(
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(TableColumns.SoundView.CREATED_AT),
                        count(PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(likeQuery()).as(TableColumns.SoundView.USER_LIKE),
                        exists(HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(PostedPlaylistMapper.HAS_PENDING_DOWNLOAD_REQUEST),
                        exists(IS_MARKED_FOR_OFFLINE_QUERY).as(PostedPlaylistMapper.IS_MARKED_FOR_OFFLINE))
                .leftJoin(Table.PlaylistTracks.name(), SoundView.field(TableColumns.SoundView._ID), PLAYLIST_ID)
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), SoundView.field(TableColumns.SoundView._TYPE)))
                .leftJoin(Table.TrackDownloads.name(), PlaylistTracks.field(TRACK_ID), Table.TrackDownloads.field(TableColumns.TrackDownloads._ID))
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(SoundView.field(TableColumns.SoundView.CREATED_AT), fromTimestamp)
                .groupBy(SoundView.field(TableColumns.SoundView._ID))
                .order(CREATED_AT, DESC)
                .limit(limit);
    }

    public static Query likeQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }
}
