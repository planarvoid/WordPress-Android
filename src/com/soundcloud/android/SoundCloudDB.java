package com.soundcloud.android;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.provider.DatabaseHelper.Users;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public enum WriteState {
        insert_only, update_only, all
    }

    public static void writeTrack(ContentResolver contentResolver, Track track, WriteState writeState, Long currentUserId) {
            Cursor cursor = contentResolver.query(track.toUri(), null, null, null, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    Log.i(TAG,"FOUND TRACK " + cursor.getCount() + " " + cursor.getColumnCount());
                    if (writeState == WriteState.update_only || writeState == WriteState.all)
                        contentResolver.update(Content.TRACKS, track.buildContentValues(), Tracks.ID
                                + "='" + track.id + "'", null);
                } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                    contentResolver.insert(Content.TRACKS, track.buildContentValues());
                }

                cursor.close();

                // write with insert only because a track will never come in
                // with
                writeUser(contentResolver, track.user, WriteState.insert_only, currentUserId);
            }
    }

    // ---Make sure the database is up to date with this track info---
    public static Track getTrackById(ContentResolver contentResolver, long trackId, long currentUserId) {

        Cursor cursor = contentResolver.query(Content.TRACKS, null, Tracks.ID + " = ?",
                new String[]{Long.toString(trackId)}, null);

        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            Track track = new Track(cursor, false);
            cursor.close();
            track.updateUserPlayedFromDb(contentResolver, currentUserId);

            User user = getUserById(contentResolver, track.user_id);
            if (user != null) {
                track.user = user;
                track.user_id = user.id;
            }
            return track;
        }

        cursor.close();
        return null;
    }

    public static boolean isTrackInDb(ContentResolver contentResolver, long id) {
        boolean ret = false;
        Cursor cursor = contentResolver.query(Content.TRACKS, null, Tracks.ID + " = ?",new String[]{Long.toString(id)}, null);
        if (null != cursor && cursor.moveToNext()) {
            ret = true;
        }

        if (cursor != null)
            cursor.close();
        return ret;
    }

    public static void writeUser(ContentResolver contentResolver,User user, WriteState writeState, Long currentUserId) {
        Cursor cursor = contentResolver.query(user.toUri(), null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (writeState == WriteState.update_only || writeState == WriteState.all)
                    contentResolver.update(Content.USERS, user.buildContentValues(currentUserId.compareTo(user.id) == 0), Users.ID + "='" + user.id + "'", null);
            } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                contentResolver.insert(Content.USERS, user.buildContentValues(currentUserId.compareTo(user.id) == 0));
            }
            cursor.close();
        }
    }

    public static User getUserById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(Content.USERS, null, Users.ID + "= ?",new String[]{Long.toString(userId)}, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor, false);
        }
        if (cursor != null) cursor.close();
        return user;
    }

    public static boolean isUserInDb(ContentResolver contentResolver, long id) {
        boolean ret = false;
        Cursor cursor = contentResolver.query(Content.USERS, null, Users.ID + " = ?",new String[]{Long.toString(id)}, null);
        if (null != cursor && cursor.moveToNext()) {
            ret = true;
        }
        if (cursor != null) cursor.close();
        return ret;
    }
}
