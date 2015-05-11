package com.soundcloud.android.testsupport.fixtures;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.sync.posts.ApiPost;
import org.hamcrest.Matchers;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

public class DatabaseFixtures {

    private final SQLiteDatabase database;

    public DatabaseFixtures(SQLiteDatabase database) {
        this.database = database;
    }

    public ApiTrack insertTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser((ApiUser) track.getUser());
        insertTrack(track);
        return track;
    }

    public long insertTrack(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, track.getId());
        cv.put(TableColumns.Sounds.TITLE, track.getTitle());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Sounds.USER_ID, track.getUser().getUrn().getNumericId());
        cv.put(TableColumns.Sounds.DURATION, track.getDuration());
        cv.put(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl());
        cv.put(TableColumns.Sounds.STREAM_URL, track.getStreamUrl());
        cv.put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount());
        cv.put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount());
        cv.put(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount());
        cv.put(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl());
        cv.put(TableColumns.Sounds.SHARING, track.getSharing().value());
        cv.put(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime());

        final long id = insertInto(Table.Sounds, cv);
        track.setId(id);

        insertPolicy(track);
        return id;
    }

    private long insertPolicy(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, track.getId());
        cv.put(TableColumns.TrackPolicies.POLICY, track.getPolicy());
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, track.isMonetizable());
        cv.put(TableColumns.TrackPolicies.SYNCABLE, track.isSyncable());
        cv.put(TableColumns.TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        return insertInto(Table.TrackPolicies, cv);
    }

    public long insertDescription(Urn trackUrn, String description) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, trackUrn.getNumericId());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Sounds.DESCRIPTION, description);
        return insertInto(Table.Sounds, cv);
    }

    public ApiPlaylist insertPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertEmptyPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTrackCount(0);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertLocalPlaylist() {
        return insertLocalPlaylist(ModelFixtures.create(ApiPlaylist.class));
    }

    public ApiPlaylist insertLocalPlaylist(ApiPlaylist playlist) {
        playlist.setUrn("soundcloud:playlists:-" + 1000 + playlist.getId());
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public long insertPlaylist(ApiPlaylist playlist) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, playlist.getId());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Sounds.TITLE, playlist.getTitle());
        cv.put(TableColumns.Sounds.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount());
        cv.put(TableColumns.Sounds.PERMALINK_URL, playlist.getPermalinkUrl());
        cv.put(TableColumns.Sounds.SHARING, playlist.getSharing().value());
        cv.put(TableColumns.Sounds.DURATION, playlist.getDuration());
        cv.put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount());
        cv.put(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime());

        final long id = insertInto(Table.Sounds, cv);
        playlist.setId(id);
        return id;
    }

    public ApiTrack insertPlaylistTrack(ApiPlaylist playlist, int position) {
        return insertPlaylistTrack(playlist.getUrn(), position);
    }

    public ApiTrack insertPlaylistTrack(Urn playlistUrn, int position) {
        ApiTrack apiTrack = insertTrack();
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, apiTrack.getId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        insertInto(Table.PlaylistTracks, cv);
        return apiTrack;
    }

    public long insertPlaylistTrack(Urn playlistUrn, Urn trackUrn, int position) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        return insertInto(Table.PlaylistTracks, cv);
    }

    public ApiTrack insertPlaylistTrackPendingAddition(ApiPlaylist playlist, int position, Date additionDate) {
        final ApiTrack apiTrack = insertPlaylistTrack(playlist, position);
        database.execSQL("UPDATE PlaylistTracks SET added_at=" + additionDate.getTime()
                + " WHERE playlist_id=" + playlist.getId()
                + " AND track_id=" + apiTrack.getId());
        return apiTrack;
    }

    public ApiTrack insertPlaylistTrackPendingRemoval(ApiPlaylist playlist, int position, Date removalDate) {
        final ApiTrack apiTrack = insertPlaylistTrack(playlist, position);
        database.execSQL("UPDATE PlaylistTracks SET removed_at=" + removalDate.getTime()
                + " WHERE playlist_id=" + playlist.getId()
                + " AND track_id=" + apiTrack.getId());
        return apiTrack;
    }

    public ApiPlaylist insertPlaylistWithCreationDate(ApiUser user, Date createdAtDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setCreatedAt(createdAtDate);
        playlist.setUser(user);
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiUser insertUser() {
        final ApiUser user = ModelFixtures.create(ApiUser.class);
        insertUser(user);
        return user;
    }

    public long insertUser(ApiUser user) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Users._ID, user.getUrn().getNumericId());
        cv.put(TableColumns.Users.USERNAME, user.getUsername());

        final long id = insertInto(Table.Users, cv);
        user.setId(id);
        return id;
    }

    public long insertLike(long id, int type, Date createdAt) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Likes._ID, id);
        cv.put(TableColumns.Likes._TYPE, type);
        cv.put(TableColumns.Likes.CREATED_AT, createdAt.getTime());
        return insertInto(Table.Likes, cv);
    }

    public long insertLike(ApiLike like) {
        return insertLike(
                like.getTargetUrn().getNumericId(),
                like.getTargetUrn().isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST,
                like.getCreatedAt());
    }

    public ApiLike insertTrackLike() {
        final ApiLike like = ModelFixtures.apiTrackLike();
        insertLike(like);
        return like;
    }

    public ApiTrack insertLikedTrack(Date likedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser((ApiUser) track.getUser());
        insertLike(insertTrack(track), TableColumns.Sounds.TYPE_TRACK, likedDate);
        return track;
    }

    public ApiTrack insertLikedTrackPendingRemoval(Date unlikedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertLike(insertTrack(track), TableColumns.Sounds.TYPE_TRACK, new Date(0));
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                + " WHERE _id=" + track.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiTrack insertLikedTrackPendingAddition(Date likedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertLike(insertTrack(track), TableColumns.Sounds.TYPE_TRACK, new Date(0));
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                + " WHERE _id=" + track.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiPlaylist insertLikedPlaylist(Date likedDate) {
        ApiPlaylist playlist = insertPlaylist();
        insertLike(insertPlaylist(playlist), TableColumns.Sounds.TYPE_PLAYLIST, likedDate);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylistPendingRemoval(Date unlikedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertLike(insertPlaylist(playlist), TableColumns.Sounds.TYPE_PLAYLIST, new Date(0));
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                + " WHERE _id=" + playlist.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylistPendingAddition(Date likedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertLike(insertPlaylist(playlist), TableColumns.Sounds.TYPE_PLAYLIST, new Date(0));
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                + " WHERE _id=" + playlist.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPost insertTrackPost(ApiPost apiTrackPost) {
        insertTrackPost(apiTrackPost.getTargetUrn().getNumericId(),
                apiTrackPost.getCreatedAt().getTime(),
                false);
        return apiTrackPost;
    }

    public long insertTrackRepost(long id, long createdAt) {
        return insertTrackPost(id, createdAt, true);
    }

    public long insertTrackPost(long id, long createdAt, boolean isRepost) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Posts.TARGET_ID, id);
        cv.put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Posts.TYPE, isRepost ? TableColumns.Posts.TYPE_REPOST : TableColumns.Posts.TYPE_POST);
        cv.put(TableColumns.Posts.CREATED_AT, createdAt);
        return insertInto(Table.Posts, cv);
    }

    public long insertPlaylistRepost(long id, long createdAt) {
        return insertPlaylistPost(id, createdAt, true);
    }

    public long insertPlaylistPost(long playlistId, long createdAt, boolean isRepost) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Posts.TARGET_ID, playlistId);
        cv.put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Posts.TYPE, isRepost ? TableColumns.Posts.TYPE_REPOST : TableColumns.Posts.TYPE_POST);
        cv.put(TableColumns.Posts.CREATED_AT, createdAt);
        return insertInto(Table.Posts, cv);
    }

    public long insertStreamTrackPost(ApiStreamItem apiStreamItem) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, apiStreamItem.getTrack().get().getId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, apiStreamItem.getTrack().get().getCreatedAt().getTime());
        return insertInto(Table.SoundStream, cv);
    }

    public long insertStreamTrackPost(long trackId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertStreamTrackRepost(long trackId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertStreamPlaylistPost(long playlistId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertStreamPlaylistRepost(long playlistId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        return insertInto(Table.SoundStream, cv);
    }

    public ApiTrack insertPromotedStreamTrack(long timestamp) {
        ApiTrack promotedTrack = insertTrack();
        long promotedId = 26;

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, promotedTrack.getUrn().getNumericId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.PROMOTED_ID, promotedId);
        insertInto(Table.SoundStream, cv);

        insertPromotedTrackMetadata(promotedId);
        return promotedTrack;
    }

    public void insertPromotedTrackMetadata(long promotedId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PromotedTracks._ID, promotedId);
        cv.put(TableColumns.PromotedTracks.AD_URN, "promoted:track:123");
        cv.put(TableColumns.PromotedTracks.PROMOTER_ID, 83);
        cv.put(TableColumns.PromotedTracks.PROMOTER_NAME, "SoundCloud");
        insertInto(Table.PromotedTracks, cv);
    }

    public void insertPolicyAllow(Urn urn, long lastUpdate) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(TableColumns.TrackPolicies.POLICY, "ALLOW");
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, true);
        cv.put(TableColumns.TrackPolicies.SYNCABLE, true);
        cv.put(TableColumns.TrackPolicies.LAST_UPDATED, lastUpdate);

        insertInto(Table.TrackPolicies, cv);
    }

    public void insertPolicyBlock(Urn urn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(TableColumns.TrackPolicies.POLICY, "BLOCK");
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, false);
        cv.put(TableColumns.TrackPolicies.SYNCABLE, false);

        insertInto(Table.TrackPolicies, cv);
    }

    public ApiTrack insertLikedTrackPendingDownload(Date likeDate) {
        ApiTrack apiTrack = insertLikedTrack(likeDate);
        insertTrackPendingDownload(apiTrack.getUrn(), System.currentTimeMillis());
        return apiTrack;
    }

    public long insertTrackPendingDownload(Urn trackUrn, long requestedAt) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, requestedAt);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertCompletedTrackDownload(Urn trackUrn, long requestedAtTimestamp, long completedTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TableColumns.TrackDownloads.DOWNLOADED_AT, completedTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertUnavailableTrackDownload(Urn trackUrn, long unavailableTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, 33333333L);
        cv.put(TableColumns.TrackDownloads.UNAVAILABLE_AT, unavailableTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertTrackDownloadPendingRemoval(Urn trackUrn, long removedAtTimestamp) {
        return insertTrackDownloadPendingRemoval(trackUrn, 0, removedAtTimestamp);
    }

    public long insertTrackDownloadPendingRemoval(Urn trackUrn, long requestedAtTimestamp, long removedAtTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TableColumns.TrackDownloads.DOWNLOADED_AT, requestedAtTimestamp);
        cv.put(TableColumns.TrackDownloads.REMOVED_AT, removedAtTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertInto(Table table, ContentValues cv) {
        final long id = database.insertWithOnConflict(table.name(), null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        assertThat(id, Matchers.not(Matchers.equalTo(-1L)));
        return id;
    }

    public ApiPlaylist insertPlaylistMarkedForOfflineSync() {
        final ApiPlaylist apiPlaylist = insertPlaylist();
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.OfflineContent._ID, apiPlaylist.getUrn().getNumericId());
        cv.put(TableColumns.OfflineContent._TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);
        insertInto(Table.OfflineContent, cv);
        return apiPlaylist;
    }

    public long insertPlaylistMarkedForOfflineSync(ApiPlaylist playlist) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.OfflineContent._ID, playlist.getUrn().getNumericId());
        cv.put(TableColumns.OfflineContent._TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);
        return insertInto(Table.OfflineContent, cv);
    }
}
