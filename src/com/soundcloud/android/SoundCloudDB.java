package com.soundcloud.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.model.ModelBase;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Tracks;
import com.soundcloud.android.provider.DBHelper.Users;

import android.content.ContentResolver;
import android.database.Cursor;
import com.soundcloud.android.provider.ScContentProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public enum WriteState {
        insert_only, update_only, all
    }

    public static void writeTrack(ContentResolver contentResolver, Track track, WriteState writeState, Long currentUserId) {
            Cursor cursor = contentResolver.query(track.toUri(), null, null, null, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    if (writeState == WriteState.update_only || writeState == WriteState.all)
                        contentResolver.update(track.toUri(), track.buildContentValues(), null,null);
                } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                    contentResolver.insert(ScContentProvider.Content.TRACKS, track.buildContentValues());
                }

                cursor.close();

                // write with insert only because a track will never come in
                // with
                writeUser(contentResolver, track.user, WriteState.insert_only, currentUserId);
            }
    }


    // ---Make sure the database is up to date with this track info---
    public static Track getTrackById(ContentResolver contentResolver, long trackId, long currentUserId) {

        Cursor cursor = contentResolver.query(ScContentProvider.Content.TRACKS.buildUpon().appendPath(Long.toString(trackId)).build(), null, null,null, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            Track track = new Track(cursor);
            cursor.close();
            track.updateUserPlayedFromDb(contentResolver, currentUserId);

            User user = getUserById(contentResolver, track.user_id);
            if (user != null) {
                track.user = user;
                track.user_id = user.id;
            }
            return track;
        }

        if (cursor != null) cursor.close();
        return null;
    }

    public static boolean isTrackInDb(ContentResolver contentResolver, long id) {
        boolean ret = false;
        Cursor cursor = contentResolver.query(ScContentProvider.Content.TRACKS.buildUpon().appendPath(Long.toString(id)).build(), null, null,null, null);
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
                    contentResolver.update(user.toUri(), user.buildContentValues(currentUserId.compareTo(user.id) == 0), null, null);
            } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                contentResolver.insert(ScContentProvider.Content.USERS, user.buildContentValues(currentUserId.compareTo(user.id) == 0));
            }
            cursor.close();
        }
    }

    public static User getUserById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(ScContentProvider.Content.USERS.buildUpon().appendPath(String.valueOf(userId)).build(), null, null,null, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }

    public static User getUserByUsername(ContentResolver contentResolver, String username) {
        Cursor cursor = contentResolver.query(ScContentProvider.Content.USERS, null, Users.USERNAME + "= ?",new String[]{username}, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }


    public static String getUsernameById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(ScContentProvider.Content.USER_ITEM, new String[]{Users.CONCRETE_USERNAME}, Users.ID + "= ?",new String[]{Long.toString(userId)}, null);
        String username = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            username = cursor.getString(0);
        }
        if (cursor != null) cursor.close();
        return username;
    }

    public static boolean isUserInDb(ContentResolver contentResolver, long userId) {
        boolean ret = false;
        Cursor cursor = contentResolver.query(ScContentProvider.Content.USERS.buildUpon().appendPath(String.valueOf(userId)).build(), null, null,null, null);
        if (null != cursor && cursor.moveToNext()) {
            ret = true;
        }
        if (cursor != null) cursor.close();
        return ret;
    }

    public static void setTrackIsFavorite(ContentResolver contentResolver, long trackId, boolean isFavorite, long currentUserId){
        Track t = SoundCloudDB.getTrackById(contentResolver, trackId, currentUserId);
        if (t != null){
            t.user_favorite = isFavorite;
            SoundCloudDB.writeTrack(contentResolver,t, SoundCloudDB.WriteState.update_only,currentUserId);
        }
    }

    public static void bulkInsertParcelables(SoundCloudApplication app, List<Parcelable> items, Uri collectionUri, long owner, int startIndex) {
        int i = 0;
        ContentValues[] bulkValues = new ContentValues[items.size()];
        Set<User> usersToInsert = new HashSet<User>();
        Set<Track> tracksToInsert = new HashSet<Track>();

        for (Parcelable p : items) {
            if (p instanceof User) {
                usersToInsert.add((User) p);
            } else if (p instanceof Track) {
                usersToInsert.add(((Track) p).user);
                tracksToInsert.add((Track) p);
            }

            ContentValues itemCv = new ContentValues();
            itemCv.put(DBHelper.CollectionItems.USER_ID, owner);
            itemCv.put(DBHelper.CollectionItems.POSITION, startIndex + i);
            itemCv.put(DBHelper.CollectionItems.ITEM_ID, ((ModelBase) p).id);
            bulkValues[i] = itemCv;
            i++;
        }

        ContentValues[] tracksCv = new ContentValues[tracksToInsert.size()];
        i = 0;
        for (Track t : tracksToInsert) {
            tracksCv[i] = t.buildContentValues();
            i++;
        }
        ContentValues[] usersCv = new ContentValues[usersToInsert.size()];
        i = 0;
        for (User u : usersToInsert) {
            usersCv[i] = u.buildContentValues();
            i++;
        }

        int inserted = app.getContentResolver().bulkInsert(ScContentProvider.Content.TRACKS, tracksCv);
        Log.i(TAG,inserted + " tracks bulk inserted");

        inserted = app.getContentResolver().bulkInsert(ScContentProvider.Content.USERS, usersCv);
        Log.i(TAG,inserted + " users bulk inserted");

        inserted = app.getContentResolver().bulkInsert(collectionUri, bulkValues);
        Log.i(TAG,inserted + " colleciton items bulk inserted");

    }
}
