package com.soundcloud.android.robolectric;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.storage.provider.Table;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.hamcrest.Matchers;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper {

    private final SQLiteDatabase database;

    public DatabaseHelper(SQLiteDatabase database) {
        this.database = database;
    }

    public TrackSummary insertTrack() throws CreateModelException {
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        insertUser(track.getUser());
        insertTrack(track);
        return track;
    }

    public long insertTrack(TrackSummary track) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Sounds._ID, track.getId());
        cv.put(DBHelper.Sounds.TITLE, track.getTitle());
        cv.put(DBHelper.Sounds._TYPE, Playable.DB_TYPE_TRACK);

        final long id = insertInto(Table.SOUNDS, cv);
        track.setId(id);
        return id;
    }

    public PlaylistSummary insertPlaylist() throws CreateModelException {
        PlaylistSummary playlist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public long insertPlaylist(PlaylistSummary playlist) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Sounds._ID, playlist.getId());
        cv.put(DBHelper.Sounds._TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Sounds.TITLE, playlist.getTitle());

        final long id = insertInto(Table.SOUNDS, cv);
        playlist.setId(id);
        return id;
    }

    public UserSummary insertUser() throws CreateModelException {
        final UserSummary user = TestHelper.getModelFactory().createModel(UserSummary.class);
        insertUser(user);
        return user;
    }

    public long insertUser(UserSummary user) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, user.getId());
        cv.put(DBHelper.Users.USERNAME, user.getUsername());

        final long id = insertInto(Table.USERS, cv);
        user.setId(id);
        return id;
    }

    public long insertLike(long soundId, long userId) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, soundId);
        cv.put(DBHelper.CollectionItems.USER_ID, userId);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, ScContentProvider.CollectionItemTypes.LIKE);
        return insertInto(Table.COLLECTION_ITEMS, cv);
    }

    public long insertTrackPost(TrackSummary track, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, track.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(DBHelper.Activities.TYPE, "track");
        cv.put(DBHelper.Activities.USER_ID, track.getUser().getId());
        cv.put(DBHelper.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepost(TrackSummary track, UserSummary reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, track.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(DBHelper.Activities.TYPE, "track-repost");
        cv.put(DBHelper.Activities.USER_ID, reposter.getId());
        cv.put(DBHelper.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistPost(PlaylistSummary playlist, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, playlist.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Activities.TYPE, "playlist");
        cv.put(DBHelper.Activities.USER_ID, playlist.getUser().getId());
        cv.put(DBHelper.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistRepost(PlaylistSummary playlist, UserSummary reposter, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, playlist.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Activities.TYPE, "playlist-repost");
        cv.put(DBHelper.Activities.USER_ID, reposter.getId());
        cv.put(DBHelper.Activities.CREATED_AT, timestamp);
        return insertInto(Table.ACTIVITIES, cv);
    }

    private long insertInto(Table table, ContentValues cv) {
        final long id = database.insertWithOnConflict(table.name, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        assertThat(id, Matchers.greaterThanOrEqualTo(0L));
        return id;
    }
}
