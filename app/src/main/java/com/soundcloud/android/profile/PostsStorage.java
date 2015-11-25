package com.soundcloud.android.profile;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.Posts;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Table.Users;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.OfflinePlaylistMapper;
import com.soundcloud.android.playlists.PlaylistMapper;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func2;

import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class PostsStorage {

    private final PropellerRx propellerRx;
    private final AccountOperations accountOperations;

    private final static String LIKED_ID = "liked_id";

    private static final Func2<List<PropertySet>, String, List<PropertySet>> COMBINE_REPOSTER = new Func2<List<PropertySet>, String, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets, String username) {
            for (PropertySet propertySet : propertySets) {
                if (propertySet.getOrElse(PlayableProperty.IS_REPOSTED, false)) {
                    propertySet.put(PostProperty.REPOSTER, username);
                }
            }
            return propertySets;
        }
    };

    @Inject
    public PostsStorage(PropellerRx propellerRx, AccountOperations accountOperations) {
        this.propellerRx = propellerRx;
        this.accountOperations = accountOperations;
    }

    Observable<List<PropertySet>> loadPostsForPlayback() {
        return propellerRx.query(buildQueryForPlayback()).map(new PostsForPlaybackMapper()).toList();
    }

    Observable<List<PropertySet>> loadPosts(int limit, long fromTimestamp){
        return Observable.zip(
                propellerRx.query(buildPostsQuery(limit, fromTimestamp)).map(new PostsMapper()).toList(),
                propellerRx.query(buildUserQuery()).map(RxResultMapper.scalar(String.class))
                        .firstOrDefault(ScTextUtils.EMPTY_STRING),
                COMBINE_REPOSTER
        );
    }

    private Query buildQueryForPlayback() {
        return Query.from(Posts.name())
                .select(
                        field(SoundView.field(TableColumns.SoundView._TYPE)).as(TableColumns.SoundView._TYPE),
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(Posts.field(TableColumns.Posts.TYPE)).as(TableColumns.Posts.TYPE))
                .innerJoin(SoundView.name(),
                        on(SoundView.field(TableColumns.SoundView._ID), Posts.field(TableColumns.Posts.TARGET_ID))
                                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Posts.field(TableColumns.Posts.TARGET_TYPE)))
                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .groupBy(SoundView.field(TableColumns.SoundView._ID) + "," + SoundView.field(TableColumns.SoundView._TYPE))
                .order(Table.Posts.field(TableColumns.Posts.CREATED_AT), DESC);
    }

    private Query buildPostsQuery(int limit, long fromTimestamp) {
        return Query.from(Posts.name())
                .select(
                        field(SoundView.field(TableColumns.SoundView._TYPE)).as(TableColumns.SoundView._TYPE),
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(SoundView.field(TableColumns.SoundView.DURATION)).as(TableColumns.SoundView.DURATION),
                        field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(TableColumns.SoundView.PLAYBACK_COUNT),
                        field(Posts.field(TableColumns.Posts.TYPE)).as(TableColumns.Posts.TYPE),
                        field(Posts.field(TableColumns.Posts.CREATED_AT)).as(TableColumns.Posts.CREATED_AT),
                        field(Likes.field(TableColumns.Likes._ID)).as(LIKED_ID),
                        count(PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT))
                .innerJoin(SoundView.name(),
                        on(SoundView.field(TableColumns.SoundView._ID), Posts.field(TableColumns.Posts.TARGET_ID))
                                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Posts.field(TableColumns.Posts.TARGET_TYPE)))
                .leftJoin(Table.PlaylistTracks.name(), playlistTracksFilter())
                .leftJoin(Likes.name(),
                        on(SoundView.field(TableColumns.SoundView._ID), Likes.field(TableColumns.Likes._ID))
                                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Likes.field(TableColumns.Likes._TYPE)))
                .whereLt(Posts.field(TableColumns.Posts.CREATED_AT), fromTimestamp)
                .groupBy(SoundView.field(TableColumns.SoundView._ID) + "," + SoundView.field(TableColumns.SoundView._TYPE))
                .order(Table.Posts.field(TableColumns.Posts.CREATED_AT), DESC)
                .limit(limit);
    }

    private Query buildUserQuery() {
        return Query.from(Users.name())
                .select(field(Users.field(TableColumns.Users.USERNAME)))
                .whereEq(Users.field(TableColumns.Users._ID), accountOperations.getLoggedInUserUrn().getNumericId());
    }

    @NonNull
    private Where playlistTracksFilter() {
        return filter()
                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(SoundView.field(TableColumns.SoundView._ID), PLAYLIST_ID);
    }

    private static class PostsMapper extends RxResultMapper<PropertySet> {

        private PostedPlaylistMapper playlistMapper = new PostedPlaylistMapper();
        private PostedTracksMapper postedTracksMapper = new PostedTracksMapper();

        @Override
        public PropertySet map(CursorReader reader) {
            if (reader.getInt(TableColumns.SoundView._TYPE) == TableColumns.Sounds.TYPE_TRACK){
                return postedTracksMapper.map(reader);
            } else {
                return playlistMapper.map(reader);
            }
        }
    }

    private static class PostedPlaylistMapper extends OfflinePlaylistMapper {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(PlaylistProperty.URN, Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(PlaylistProperty.TRACK_COUNT, readTrackCount(cursorReader));
            propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            propertySet.put(PlaylistProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
            propertySet.put(PlayableProperty.IS_LIKED, cursorReader.isNotNull(LIKED_ID));
            propertySet.put(PlayableProperty.IS_REPOSTED, TableColumns.Posts.TYPE_REPOST.equals(cursorReader.getString(TableColumns.Posts.TYPE)));
            propertySet.put(PostProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Posts.CREATED_AT));
            return propertySet;
        }

        int readTrackCount(CursorReader cursorReader) {
            return Math.max(cursorReader.getInt(PlaylistMapper.LOCAL_TRACK_COUNT),
                    cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
        }
    }

    private static class PostedTracksMapper extends OfflinePlaylistMapper {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(TrackProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(TrackProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(TrackProperty.PLAY_DURATION, cursorReader.getLong(TableColumns.SoundView.DURATION));
            propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
            propertySet.put(TrackProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            propertySet.put(TrackProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
            propertySet.put(PlayableProperty.IS_LIKED, cursorReader.isNotNull(LIKED_ID));
            propertySet.put(PlayableProperty.IS_REPOSTED, TableColumns.Posts.TYPE_REPOST.equals(cursorReader.getString(TableColumns.Posts.TYPE)));
            propertySet.put(PostProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Posts.CREATED_AT));

            return propertySet;
        }
    }

    private class PostsForPlaybackMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            if (cursorReader.getInt(TableColumns.SoundView._TYPE) == TableColumns.Sounds.TYPE_TRACK){
                propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(BaseColumns._ID)));
            } else {
                propertySet.put(PlaylistProperty.URN, Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID)));
            }
            if (TableColumns.Posts.TYPE_REPOST.equals(cursorReader.getString(TableColumns.Posts.TYPE))){
                propertySet.put(PostProperty.REPOSTER_URN, accountOperations.getLoggedInUserUrn());
            }
            return propertySet;
        }
    }
}
