package com.soundcloud.android.testsupport.fixtures;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
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
        insertUser(track.getUser());
        insertTrack(track);
        return track;
    }

    public long insertTrack(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, track.getId());
        cv.put(TableColumns.Sounds.TITLE, track.getTitle());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Sounds.USER_ID, track.getUser().getId());
        cv.put(TableColumns.Sounds.DURATION, track.getDuration());
        cv.put(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl());
        cv.put(TableColumns.Sounds.STREAM_URL, track.getStreamUrl());
        cv.put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount());
        cv.put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount());
        cv.put(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount());
        cv.put(TableColumns.Sounds.POLICY, track.getPolicy());
        cv.put(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl());
        cv.put(TableColumns.Sounds.SHARING, track.getSharing().value());
        cv.put(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime());

        final long id = insertInto(Table.Sounds, cv);
        track.setId(id);
        return id;
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

    public long insertPlaylist(ApiPlaylist playlist) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, playlist.getId());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Sounds.TITLE, playlist.getTitle());
        cv.put(TableColumns.Sounds.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.DURATION, playlist.getDuration());
        cv.put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount());

        final long id = insertInto(Table.Sounds, cv);
        playlist.setId(id);
        return id;
    }

    public ApiTrack insertPlaylistTrack(ApiPlaylist playlist, int position) {
        ApiTrack apiTrack = insertTrack();
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, apiTrack.getId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        insertInto(Table.PlaylistTracks, cv);
        return apiTrack;
    }

    public ApiUser insertUser() {
        final ApiUser user = ModelFixtures.create(ApiUser.class);
        insertUser(user);
        return user;
    }

    public long insertUser(ApiUser user) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Users._ID, user.getId());
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
        insertUser(track.getUser());
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

    public ApiLike insertPlaylistLike() {
        final ApiLike like = ModelFixtures.apiPlaylistLike();
        insertLike(like);
        return like;
    }

    public long insertLegacyPlaylistLike(long playlistId, long userId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionItems.ITEM_ID, playlistId);
        cv.put(TableColumns.CollectionItems.USER_ID, userId);
        cv.put(TableColumns.CollectionItems.COLLECTION_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.CollectionItems.RESOURCE_TYPE, CollectionStorage.CollectionItemTypes.LIKE);
        return insertInto(Table.CollectionItems, cv);
    }

    @Deprecated
    public long insertLegacyTrackPost(ApiTrack track, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track");
        cv.put(TableColumns.Activities.USER_ID, track.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.Activities, cv);
    }

    @Deprecated
    public long insertLegacyTrackRepost(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.Activities, cv);
    }

    public long insertTrackRepostOfOwnTrack(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.Activities, cv);
    }

    @Deprecated
    public long insertLegacyPlaylistPost(ApiPlaylist playlist, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist");
        cv.put(TableColumns.Activities.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.Activities, cv);
    }

    @Deprecated
    public long insertLegacyPlaylistRepost(ApiPlaylist playlist, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE,TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.Activities, cv);
    }

    public long insertComment(PublicApiComment comment) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.COMMENT_ID, comment.getId());
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.SOUND_ID, comment.track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "comment");
        cv.put(TableColumns.Activities.USER_ID, comment.user.getId());
        cv.put(TableColumns.Activities.CREATED_AT, comment.getCreatedAt().getTime());
        return insertInto(Table.Activities, cv);
    }

    public PublicApiComment insertComment() {
        PublicApiComment comment = ModelFixtures.create(PublicApiComment.class);
        insertComment(comment);
        return comment;
    }

    public long insertAffiliation(AffiliationActivity affiliation) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.TYPE, "affiliation");
        cv.put(TableColumns.Activities.USER_ID, affiliation.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, affiliation.getCreatedAt().getTime());
        return insertInto(Table.Activities, cv);
    }

    public AffiliationActivity insertAffiliation() {
        AffiliationActivity affiliation = ModelFixtures.create(AffiliationActivity.class);
        insertAffiliation(affiliation);
        return affiliation;
    }

    public long insertStreamTrackPost(ApiStreamItem apiStreamItem) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, apiStreamItem.getTrack().get().getId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, apiStreamItem.getTrack().get().getCreatedAt().getTime());
        return insertInto(Table.SoundStream, cv);
    }

    public long insertTrackPost(long trackId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertTrackRepost(long trackId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertPlaylistPost(long playlistId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertPlaylistRepost(long playlistId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        return insertInto(Table.SoundStream, cv);
    }

    public long insertRequestedTrackDownload(Urn trackUrn, long addedTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, addedTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertCompletedTrackDownload(Urn trackUrn, long completedTimestamp) {
        return insertCompletedTrackDownload(trackUrn, completedTimestamp, completedTimestamp);
    }

    public long insertCompletedTrackDownload(Urn trackUrn, long requestedAtTimestamp, long completedTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TableColumns.TrackDownloads.DOWNLOADED_AT, completedTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertTrackDownloadPendingRemoval(Urn trackUrn, long removedAtTimestamp) {
        return insertTrackDownloadPendingRemoval(trackUrn, removedAtTimestamp, removedAtTimestamp);
    }

    public long insertTrackDownloadPendingRemoval(Urn trackUrn, long requestedAtTimestamp, long removedAtTimestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TableColumns.TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TableColumns.TrackDownloads.REMOVED_AT, removedAtTimestamp);
        return insertInto(Table.TrackDownloads, cv);
    }

    public long insertInto(Table table, ContentValues cv) {
        final long id = database.insertWithOnConflict(table.name(), null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        assertThat(id, Matchers.greaterThanOrEqualTo(0L));
        return id;
    }
}
