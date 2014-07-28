package com.soundcloud.android.robolectric;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.hamcrest.Matchers;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper {

    private final SQLiteDatabase database;

    public DatabaseHelper(SQLiteDatabase database) {
        this.database = database;
    }

    public ApiTrack insertTrack() throws CreateModelException {
        ApiTrack track = TestHelper.getModelFactory().createModel(ApiTrack.class);
        insertUser(track.getUser());
        insertTrack(track);
        return track;
    }

    public long insertTrack(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, track.getId());
        cv.put(TableColumns.Sounds.TITLE, track.getTitle());
        cv.put(TableColumns.Sounds._TYPE, Playable.DB_TYPE_TRACK);
        cv.put(TableColumns.Sounds.USER_ID, track.getUser().getId());
        cv.put(TableColumns.Sounds.DURATION, track.getDuration());
        cv.put(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl());
        cv.put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount());
        cv.put(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl());

        final long id = insertInto(Table.SOUNDS, cv);
        track.setId(id);
        return id;
    }

    public ApiPlaylist insertPlaylist() throws CreateModelException {
        ApiPlaylist playlist = TestHelper.getModelFactory().createModel(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public long insertPlaylist(ApiPlaylist playlist) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, playlist.getId());
        cv.put(TableColumns.Sounds._TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(TableColumns.Sounds.TITLE, playlist.getTitle());
        cv.put(TableColumns.Sounds.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.DURATION, playlist.getDuration());
        cv.put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount());

        final long id = insertInto(Table.SOUNDS, cv);
        playlist.setId(id);
        return id;
    }

    public ApiTrack insertPlaylistTrack(ApiPlaylist playlist, int position) throws CreateModelException {
        ApiTrack apiTrack = insertTrack();
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, apiTrack.getId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        insertInto(Table.PLAYLIST_TRACKS, cv);
        return apiTrack;
    }

    public ApiUser insertUser() throws CreateModelException {
        final ApiUser user = TestHelper.getModelFactory().createModel(ApiUser.class);
        insertUser(user);
        return user;
    }

    public long insertUser(ApiUser user) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Users._ID, user.getId());
        cv.put(TableColumns.Users.USERNAME, user.getUsername());

        final long id = insertInto(Table.USERS, cv);
        user.setId(id);
        return id;
    }

    public long insertPlaylistLike(long playlistId, long userId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionItems.ITEM_ID, playlistId);
        cv.put(TableColumns.CollectionItems.USER_ID, userId);
        cv.put(TableColumns.CollectionItems.COLLECTION_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(TableColumns.CollectionItems.RESOURCE_TYPE, CollectionStorage.CollectionItemTypes.LIKE);
        return insertInto(Table.COLLECTION_ITEMS, cv);
    }

    public long insertTrackPost(ApiTrack track, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track");
        cv.put(TableColumns.Activities.USER_ID, track.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepost(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepostOfOwnTrack(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistPost(ApiPlaylist playlist, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist");
        cv.put(TableColumns.Activities.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistRepost(ApiPlaylist playlist, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertComment(Comment comment) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.COMMENT_ID, comment.getId());
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.SOUND_ID, comment.track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "comment");
        cv.put(TableColumns.Activities.USER_ID, comment.user.getId());
        cv.put(TableColumns.Activities.CREATED_AT, comment.getCreatedAt().getTime());
        return insertInto(Table.ACTIVITIES, cv);
    }

    public Comment insertComment() throws CreateModelException {
        Comment comment = TestHelper.getModelFactory().createModel(Comment.class);
        insertComment(comment);
        return comment;
    }

    public long insertAffiliation(AffiliationActivity affiliation) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.TYPE, "affiliation");
        cv.put(TableColumns.Activities.USER_ID, affiliation.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, affiliation.getCreatedAt().getTime());
        return insertInto(Table.ACTIVITIES, cv);
    }

    public AffiliationActivity insertAffiliation() throws CreateModelException {
        AffiliationActivity affiliation = TestHelper.getModelFactory().createModel(AffiliationActivity.class);
        insertAffiliation(affiliation);
        return affiliation;
    }

    public long insertInto(Table table, ContentValues cv) {
        final long id = database.insertWithOnConflict(table.name, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        assertThat(id, Matchers.greaterThanOrEqualTo(0L));
        return id;
    }
}
