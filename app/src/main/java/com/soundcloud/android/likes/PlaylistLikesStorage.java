package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LikedPlaylistMapper;
import com.soundcloud.android.playlists.PlaylistMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;
import rx.functions.Func1;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class PlaylistLikesStorage {

    private final DatabaseScheduler scheduler;

    private final Func1<QueryResult, PropertySet> toPropertySet = new Func1<QueryResult, PropertySet>() {
        @Override
        public PropertySet call(QueryResult queryResult) {
            return queryResult.firstOrDefault(new LikedPlaylistMapper(), PropertySet.create());
        }
    };

    @Inject
    public PlaylistLikesStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<List<PropertySet>> loadLikedPlaylists(int limit, long fromTimestamp) {
        final Query query = playlistLikeQuery()
                .whereLt(Table.Likes.field(TableColumns.Likes.CREATED_AT), fromTimestamp)
                .order(Table.Likes.field(TableColumns.Likes.CREATED_AT), Query.ORDER_DESC)
                .limit(limit);

        return scheduler.scheduleQuery(query).map(new LikedPlaylistMapper()).toList();
    }

    public Observable<PropertySet> loadLikedPlaylist(Urn urn) {
        final Query query = playlistLikeQuery().whereEq(Table.Likes + "." + TableColumns.Likes._ID, urn.getNumericId());
        return scheduler.scheduleQueryForResult(query)
                .map(toPropertySet);
    }

    static Query playlistLikeQuery() {
        final Where likesSoundViewJoin = filter()
                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), Table.SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(Table.Likes.field(TableColumns.Likes._ID), Table.SoundView.field(TableColumns.SoundView._ID));

        return Query.from(Table.Likes.name())
                .select(
                        field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID),
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.SHARING,
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        field(Table.Likes + "." + TableColumns.Likes.CREATED_AT).as(TableColumns.Likes.CREATED_AT))
                .innerJoin(Table.SoundView.name(), likesSoundViewJoin)
                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT))
                .groupBy(Table.SoundView.field(TableColumns.SoundView._ID));
    }
}
