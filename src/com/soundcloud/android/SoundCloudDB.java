package com.soundcloud.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Users;

import android.content.ContentResolver;
import android.database.Cursor;

import java.util.*;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public enum WriteState {
        insert_only, update_only, all
    }

    public static void writeTrack(ContentResolver contentResolver, Track track, WriteState writeState, long currentUserId) {
        Cursor cursor = contentResolver.query(track.toUri(), null, null, null, null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (writeState == WriteState.update_only || writeState == WriteState.all)
                    contentResolver.update(track.toUri(), track.buildContentValues(), null, null);
            } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                contentResolver.insert(Content.TRACKS.uri, track.buildContentValues());
            }

            cursor.close();
            writeUser(contentResolver, track.user, WriteState.insert_only, currentUserId);

        }
    }


    // ---Make sure the database is up to date with this track info---
    public static Track getTrackById(ContentResolver resolver, long trackId) {
        Cursor cursor = resolver.query(Content.TRACKS.buildUpon()
                .appendPath(String.valueOf(trackId)).build(), null, null, null, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            Track track = new Track(cursor);
            cursor.close();

            User user = getUserById(resolver, track.user_id);
            if (user != null) {
                track.user = user;
                track.user_id = user.id;
            }
            return track;
        }

        if (cursor != null) cursor.close();
        return null;
    }

    public static List<Track> getTracks(ContentResolver resolver, long[] ids) {
        List<Track> tracks = new ArrayList<Track>(ids.length);
        for (long id : ids) {
            tracks.add(getTrackById(resolver, id));
        }
        return tracks;
    }

    public static List<Track> getTracks(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null) {
            List<Track> tracks = new ArrayList<Track>(cursor.getCount());
            while (cursor.moveToNext()) {
                tracks.add(new Track(cursor));
            }
            cursor.close();
            return tracks;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public static boolean isTrackInDb(ContentResolver contentResolver, long id) {
        boolean ret = false;
        Cursor cursor = contentResolver.query(Content.TRACKS.buildUpon().appendPath(Long.toString(id)).build(), null, null,null, null);
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
                contentResolver.insert(Content.USERS.uri, user.buildContentValues(currentUserId.compareTo(user.id) == 0));
            }
        }
    }

    public static User getUserById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(Content.USERS.buildUpon().appendPath(String.valueOf(userId)).build(), null, null,null, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }

    public static User getUserByUsername(ContentResolver contentResolver, String username) {
        Cursor cursor = contentResolver.query(Content.USERS.uri, null, Users.USERNAME + "= ?",new String[]{username}, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }


    public static String getUsernameById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(Content.USER_ITEM.uri, new String[]{Users.USERNAME}, Users.ID + "= ?",new String[]{Long.toString(userId)}, null);
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
        Cursor cursor = contentResolver.query(Content.USERS.buildUpon().appendPath(String.valueOf(userId)).build(), null, null,null, null);
        if (null != cursor && cursor.moveToNext()) {
            ret = true;
        }
        if (cursor != null) cursor.close();
        return ret;
    }

    public static void setTrackIsFavorite(ContentResolver contentResolver, long trackId, boolean isFavorite, long currentUserId){
        Track t = SoundCloudDB.getTrackById(contentResolver, trackId);
        if (t != null){
            t.user_favorite = isFavorite;
            SoundCloudDB.writeTrack(contentResolver,t, SoundCloudDB.WriteState.update_only,currentUserId);
        }
    }

    public static int bulkInsertParcelables(SoundCloudApplication app, List<Parcelable> items) {
        return bulkInsertParcelables(app,items,null,-1,-1);
    }
    public static int bulkInsertParcelables(SoundCloudApplication app, List<Parcelable> items, Uri collectionUri, long owner, int startIndex) {

        int i = 0;
        Set<User> usersToInsert = new HashSet<User>();
        Set<Track> tracksToInsert = new HashSet<Track>();
        ContentValues[] bulkValues = null;
        if (collectionUri != null) {
            bulkValues = new ContentValues[items.size()];
        }

        // XXX lookup should be in model (Origin)
        for (Parcelable p : items) {
            long id = ((ScModel) p).id;
            Log.i("asdf","WHAT IS P?? " + p + " " + (p instanceof User));
            if (p instanceof User) {
                usersToInsert.add((User) p);
            } else if (p instanceof Track) {
                usersToInsert.add(((Track) p).user);
                tracksToInsert.add((Track) p);
            } else if (p instanceof Friend) {
                id = ((Friend) p).user.id;
                usersToInsert.add(((Friend) p).user);
            }

            if (bulkValues != null){
                ContentValues itemCv = new ContentValues();
                itemCv.put(DBHelper.CollectionItems.USER_ID, owner);
                itemCv.put(DBHelper.CollectionItems.POSITION, startIndex + i);
                itemCv.put(DBHelper.CollectionItems.ITEM_ID, id);
                bulkValues[i] = itemCv;
                i++;
            }
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

        int inserted = app.getContentResolver().bulkInsert(Content.TRACKS.uri, tracksCv);
        Log.i(TAG,inserted + " tracks bulk inserted");

        inserted += app.getContentResolver().bulkInsert(Content.USERS.uri, usersCv);
        Log.i(TAG,inserted + " users bulk inserted");

        if (bulkValues != null){
            inserted = app.getContentResolver().bulkInsert(collectionUri, bulkValues);
            Log.i(TAG,inserted + " colleciton items bulk inserted");
        }

        return inserted;
    }
}
