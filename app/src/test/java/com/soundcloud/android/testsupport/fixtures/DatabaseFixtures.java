package com.soundcloud.android.testsupport.fixtures;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import org.hamcrest.Matchers;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

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

        final long id = insertInto(Table.SOUNDS, cv);
        track.setId(id);
        return id;
    }

    public long insertDescription(Urn trackUrn, String description) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, trackUrn.getNumericId());
        cv.put(TableColumns.Sounds.DESCRIPTION, description);
        return insertInto(Table.SOUNDS, cv);
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

        final long id = insertInto(Table.SOUNDS, cv);
        playlist.setId(id);
        return id;
    }

    public ApiTrack insertPlaylistTrack(ApiPlaylist playlist, int position) {
        ApiTrack apiTrack = insertTrack();
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, apiTrack.getId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        insertInto(Table.PLAYLIST_TRACKS, cv);
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

        final long id = insertInto(Table.USERS, cv);
        user.setId(id);
        return id;
    }

    public long insertPlaylistLike(long playlistId, long userId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionItems.ITEM_ID, playlistId);
        cv.put(TableColumns.CollectionItems.USER_ID, userId);
        cv.put(TableColumns.CollectionItems.COLLECTION_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.CollectionItems.RESOURCE_TYPE, CollectionStorage.CollectionItemTypes.LIKE);
        return insertInto(Table.COLLECTION_ITEMS, cv);
    }

    public long insertTrackPost(ApiTrack track, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track");
        cv.put(TableColumns.Activities.USER_ID, track.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepost(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepostOfOwnTrack(ApiTrack track, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_ACTIVITIES.id);
        cv.put(TableColumns.Activities.SOUND_ID, track.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Activities.TYPE, "track-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistPost(ApiPlaylist playlist, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist");
        cv.put(TableColumns.Activities.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistRepost(ApiPlaylist playlist, ApiUser reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Activities.CONTENT_ID, Content.ME_SOUND_STREAM.id);
        cv.put(TableColumns.Activities.SOUND_ID, playlist.getId());
        cv.put(TableColumns.Activities.SOUND_TYPE,TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Activities.TYPE, "playlist-repost");
        cv.put(TableColumns.Activities.USER_ID, reposter.getId());
        cv.put(TableColumns.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
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
        return insertInto(Table.ACTIVITIES, cv);
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
        return insertInto(Table.ACTIVITIES, cv);
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
        return insertInto(Table.SOUNDSTREAM, cv);
    }

    public long insertInto(Table table, ContentValues cv) {
        final long id = database.insertWithOnConflict(table.name, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        assertThat(id, Matchers.greaterThanOrEqualTo(0L));
        return id;
    }
}
