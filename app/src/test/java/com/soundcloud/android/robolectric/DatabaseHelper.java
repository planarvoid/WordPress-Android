package com.soundcloud.android.robolectric;

import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.Lists;
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;

public class DatabaseHelper {

    private final SQLiteDatabase database;

    public DatabaseHelper(SQLiteDatabase database) {
        this.database = database;
    }

    public int getRowCount(Table table) {
        final Cursor cursor = database.rawQuery("select count (*) from " + table.name, null);
        cursor.moveToNext();
        return cursor.getInt(0);
    }

    public Collection<TrackSummary> insertTracks(int numTracks) throws CreateModelException {
        Collection<TrackSummary> tracks = Lists.newArrayListWithCapacity(numTracks);
        for (int i = 0; i < numTracks; i++) {
            TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
            tracks.add(track);
            insertTrack(track);
            insertUser(track.getUser());
        }

        return tracks;
    }

    public TrackSummary insertTrack() throws CreateModelException {
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        insertTrack(track);
        return track;
    }

    public long insertTrack(TrackSummary track) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Sounds._ID, track.getId());
        cv.put(DBHelper.Sounds.TITLE, track.getTitle());
        cv.put(DBHelper.Sounds._TYPE, Playable.DB_TYPE_TRACK);

        return insertInto(Table.SOUNDS, cv);
    }

    public PlaylistSummary insertPlaylist() throws CreateModelException {
        PlaylistSummary playlist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        insertPlaylist(playlist);
        return playlist;
    }

    public long insertPlaylist(PlaylistSummary playlist) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Sounds._ID, playlist.getId());
        cv.put(DBHelper.Sounds._TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Sounds.TITLE, playlist.getTitle());

        return insertInto(Table.SOUNDS, cv);
    }

    public long insertUser(UserSummary user) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, user.getId());
        cv.put(DBHelper.Users.USERNAME, user.getUsername());

        return insertInto(Table.USERS, cv);
    }

    public long insertLike(long soundId, long userId) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, soundId);
        cv.put(DBHelper.CollectionItems.USER_ID, userId);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, ScContentProvider.CollectionItemTypes.LIKE);
        return insertInto(Table.COLLECTION_ITEMS, cv);
    }

    public long insertTrackPost(TrackSummary track) throws CreateModelException {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, track.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(DBHelper.Activities.TYPE, "track");
        cv.put(DBHelper.Activities.USER_ID, track.getUser().getId());
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertTrackRepost(TrackSummary track) throws CreateModelException {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, track.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        cv.put(DBHelper.Activities.TYPE, "track-repost");
        cv.put(DBHelper.Activities.USER_ID, track.getUser().getId());
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistPost(PlaylistSummary playlist) throws CreateModelException {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, playlist.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Activities.TYPE, "playlist");
        cv.put(DBHelper.Activities.USER_ID, playlist.getUser().getId());
        return insertInto(Table.ACTIVITIES, cv);
    }

    public long insertPlaylistRepost(PlaylistSummary playlist) throws CreateModelException {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.SOUND_ID, playlist.getId());
        cv.put(DBHelper.Activities.SOUND_TYPE, Playable.DB_TYPE_PLAYLIST);
        cv.put(DBHelper.Activities.TYPE, "playlist-repost");
        cv.put(DBHelper.Activities.USER_ID, playlist.getUser().getId());
        return insertInto(Table.ACTIVITIES, cv);
    }

    private long insertInto(Table table, ContentValues cv) {
        final long id = database.insert(table.name, null, cv);
        assertThat(id, Matchers.greaterThanOrEqualTo(1L));
        return id;
    }
}
