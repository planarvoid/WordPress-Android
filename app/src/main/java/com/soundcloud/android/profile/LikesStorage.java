package com.soundcloud.android.profile;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.OfflinePlaylistMapper;
import com.soundcloud.android.playlists.PlaylistMapper;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class LikesStorage {

    private final PropellerRx propellerRx;

    @Inject
    public LikesStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<List<PropertySet>> loadLikes(int limit, long fromTimestamp){
        return propellerRx.query(buildLikesQuery(limit, fromTimestamp)).map(new LikesMapper()).toList();
    }

    Observable<List<Urn>> loadLikesForPlayback() {
        return propellerRx.query(buildQueryForPlayback()).map(new TrackUrnMapper()).toList();
    }

    private Query buildLikesQuery(int limit, long fromTimestamp) {
        return Query.from(Likes)
                .select(
                        field(SoundView.field(TableColumns.SoundView._TYPE)).as(TableColumns.SoundView._TYPE),
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(TableColumns.SoundView.PLAYBACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(SoundView.field(TableColumns.SoundView.DURATION)).as(TableColumns.SoundView.DURATION),
                        field(Likes.field(TableColumns.Likes.CREATED_AT)).as(TableColumns.Likes.CREATED_AT),
                        count(PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT))
                .innerJoin(SoundView.name(),
                        on(SoundView.field(TableColumns.SoundView._ID), Likes.field(TableColumns.Likes._ID))
                                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Likes.field(TableColumns.Likes._TYPE)))
                .leftJoin(Table.PlaylistTracks.name(), playlistTracksFilter())
                .whereLt(Likes.field(TableColumns.Likes.CREATED_AT), fromTimestamp)
                .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT))
                .groupBy(SoundView.field(TableColumns.SoundView._ID) + "," + SoundView.field(TableColumns.SoundView._TYPE))
                .order(Table.Likes.field(TableColumns.Likes.CREATED_AT), DESC)
                .limit(limit);
    }

    @NonNull
    private Where playlistTracksFilter() {
        return filter()
                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(SoundView.field(TableColumns.SoundView._ID), PLAYLIST_ID);
    }

    private Query buildQueryForPlayback() {
        return Query.from(Likes)
                .select(field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID))
                .innerJoin(SoundView.name(),
                        on(SoundView.field(TableColumns.SoundView._ID), Likes.field(TableColumns.Likes._ID))
                                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Likes.field(TableColumns.Likes._TYPE)))
                .whereEq(SoundView.field(TableColumns.SoundView._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT))
                .order(Table.Likes.field(TableColumns.Likes.CREATED_AT), DESC);
    }

    private static class LikesMapper extends RxResultMapper<PropertySet> {

        private LikedPlaylistMapper playlistMapper = new LikedPlaylistMapper();
        private LikedTrackMapper likedTrackMapper = new LikedTrackMapper();

        @Override
        public PropertySet map(CursorReader reader) {
            if (reader.getInt(TableColumns.SoundView._TYPE) == TableColumns.Sounds.TYPE_TRACK){
                return likedTrackMapper.map(reader);
            } else {
                return playlistMapper.map(reader);
            }
        }
    }

    private static class LikedPlaylistMapper extends OfflinePlaylistMapper {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(PlaylistProperty.URN, Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(PlaylistProperty.TRACK_COUNT, readTrackCount(cursorReader));
            propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            propertySet.put(PlaylistProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
            propertySet.put(PlayableProperty.IS_LIKED, true);
            propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
            return propertySet;
        }

        int readTrackCount(CursorReader cursorReader) {
            return Math.max(cursorReader.getInt(PlaylistMapper.LOCAL_TRACK_COUNT),
                    cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
        }
    }

    private static class LikedTrackMapper extends OfflinePlaylistMapper {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(TrackProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(TrackProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(TrackProperty.DURATION, cursorReader.getLong(TableColumns.SoundView.DURATION));
            propertySet.put(TrackProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
            propertySet.put(TrackProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
            propertySet.put(PlayableProperty.IS_LIKED, true);
            propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
            return propertySet;
        }
    }
}
