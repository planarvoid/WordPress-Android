package com.soundcloud.android;

import com.soundcloud.android.objects.Activities;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.provider.DatabaseHelper.Users;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public enum WriteState {
        insert_only, update_only, all
    }

    public static Integer updateActivities(SoundCloudApplication app,
                                           ContentResolver contentResolver,
                                           Long currentUserId, boolean exclusive) throws IOException {

        app.setAccountData(User.DataKeys.LAST_INCOMING_SYNC, System.currentTimeMillis());

        int maxStored = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(app)
                .getString("dashboardMaxStored", "100"));

        // get the timestamp of the newest record in the database
        Cursor firstCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
                Events.ID, Events.ORIGIN_ID,
        }, Events.USER_ID + " = ? AND " + Events.EXCLUSIVE + " = ?", new String[] {
                Long.toString(currentUserId), (exclusive ? "1" : "0")
        }, Events.ID + " DESC LIMIT 1");

        if (firstCursor.getCount() > 0) firstCursor.moveToFirst();

        final long firstTrackId = firstCursor.getCount() == 0 ? 0 : firstCursor.getLong(1);
        firstCursor.close();

        int added = 0;
        boolean caughtUp = false;
        Activities activities = null;

        List<ContentValues> tracksCV = new ArrayList<ContentValues>();
        List<ContentValues> eventsCV = new ArrayList<ContentValues>();
        List<ContentValues> usersCV = new ArrayList<ContentValues>();

        do {
            Request request = Request.to(exclusive ?
                    Endpoints.MY_EXCLUSIVE_TRACKS
                    : Endpoints.MY_ACTIVITIES).add("limit", 20);

            if (activities != null) { // add next cursor if applicable
                request.add("cursor", activities.getCursor());
            }

            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                activities = app.getMapper().readValue(response.getEntity().getContent(),
                        Activities.class);

                if (activities.size() == 0) {
                    caughtUp = true;
                    break;
                }

                activities.setCursorToLastEvent();

                for (Event evt : activities) {
                    if (evt.getTrack().id != firstTrackId) {
                        added++;

                        // TODO do not always add every user and track. keep a
                        // map to check against
                        // what was already added, otherwise unnecessary db IO

                        tracksCV.add(0, evt.getTrack().buildContentValues());
                        usersCV.add(0, evt.getTrack().user.buildContentValues(false));
                        eventsCV.add(0, evt.buildContentValues(currentUserId, exclusive));

                    } else {
                        caughtUp = true;
                        break;
                    }
                }

                if (firstTrackId <= 0 && added >= maxStored)
                    caughtUp = true;

                Log.i(TAG, "Adding " + added + " activities");
            } else {
                return 0;
            }
        } while (!caughtUp && !TextUtils.isEmpty(activities.next_href));

        if (tracksCV.size() > 0) {
            contentResolver.bulkInsert(Content.TRACKS,
                    tracksCV.toArray(new ContentValues[tracksCV.size()]));
        }
        if (usersCV.size() > 0) {
            contentResolver.bulkInsert(Content.USERS,
                    usersCV.toArray(new ContentValues[usersCV.size()]));
        }
        if (eventsCV.size() > 0) {
            contentResolver.bulkInsert(Events.CONTENT_URI,
                    eventsCV.toArray(new ContentValues[eventsCV.size()]));
        }

        return added;

    }

     // ---Make sure the database is up to date with this track info---
    public static void insertActivities(ContentResolver contentResolver, Activities activities, Long currentUserId) {
            insertActivities(contentResolver, activities, currentUserId, 0);
        }

    public static int insertActivities(ContentResolver contentResolver, Activities activities,  Long currentUserId, long stopAtTrackId) {

            List<ContentValues> tracksCV = new ArrayList<ContentValues>();
            List<ContentValues> eventsCV = new ArrayList<ContentValues>();
            List<ContentValues> usersCV = new ArrayList<ContentValues>();

            int inserted = 0;
            for (Event evt : activities) {
                if (evt.getTrack().id != stopAtTrackId){
                    inserted++;
                    tracksCV.add(0, evt.getTrack().buildContentValues());
                    eventsCV.add(0, evt.buildContentValues(currentUserId, false));
                    usersCV.add(0, evt.getTrack().user.buildContentValues(false));
                } else {
                    break;
                }
            }

            contentResolver.bulkInsert(Content.TRACKS, tracksCV.toArray(new ContentValues[tracksCV.size()]));
            contentResolver.bulkInsert(Content.USERS, usersCV.toArray(new ContentValues[usersCV.size()]));
            contentResolver.bulkInsert(Events.CONTENT_URI, eventsCV.toArray(new ContentValues[eventsCV.size()]));
            return inserted;
        }

    public static void cleanStaleActivities(ContentResolver contentResolver, Long userId, int maxEvents, boolean exclusive) {
        Cursor countCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
            "count(" + Events.ID + ")",
        }, Events.USER_ID + " = ? AND " + Events.EXCLUSIVE + " = ?", new String[] {
                Long.toString(userId), (exclusive ? "1" : "0")
        }, null);

        countCursor.moveToFirst();
        int eventsCount = countCursor.getInt(0);
        countCursor.close();

        // if there are older entries, delete them as necessary
        if (eventsCount > maxEvents) {

            Log.i(TAG,"Cleaning Stale Activities for user " + userId + ", deleting " + Math.max(0,(eventsCount - maxEvents)) + " of " + eventsCount);
            long lastId = 0;
            int limit = maxEvents;
            Cursor lastCursor = null;

            // find something with a next_cursor to keep so we have a continuous dashboard
            do {
                if (lastCursor != null) {
                    limit += 10;
                    lastCursor.close();
                }

                lastCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
                        Events.ID, Events.NEXT_CURSOR
                }, Events.USER_ID + " = ? AND " + Events.EXCLUSIVE + " = ?", new String[] {
                        Long.toString(userId), (exclusive ? "1" : "0")
                }, Events.ID + " DESC LIMIT " + limit);

                if (lastCursor != null){
                    lastCursor.moveToLast();
                    while(!lastCursor.isBeforeFirst() && TextUtils.isEmpty(lastCursor.getString(1))){
                        lastCursor.moveToPrevious();
                    }
                    lastId = lastCursor.isBeforeFirst() ? 0 : lastCursor.getLong(0);
                }
            } while (lastId == 0 && limit < eventsCount);

            lastCursor.close();

            if (lastId > 0){
                Log.i(TAG,
                        "Deleting rows before id " + lastId + " "
                                + contentResolver.delete(Events.CONTENT_URI, Events.USER_ID + " = "
                                        + userId + " AND " + Events.ID + " < " + lastId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0"),
                                        null) + " deleted ");
            } else {
                Log.i(TAG,
                        "Deleting all rows " +
                                + contentResolver.delete(Events.CONTENT_URI, Events.USER_ID + " = "
                                        + userId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0"),
                                        null));
            }

        }

    }

    public static User[] getUsersFromRecentActivities(ContentResolver contentResolver, Long userId, boolean exclusive, int limit) {
        Cursor cursor = contentResolver.query(exclusive ? Content.EXCLUSIVE_TRACKS
                : Content.INCOMING_TRACKS, null, Events.ALIAS_USER_ID + "= ? AND "
                + Events.ALIAS_EXCLUSIVE + " = ?", new String[] {
                Long.toString(userId), (exclusive ? "1" : "0")
        }, Events.ALIAS_ID + " DESC LIMIT " + limit);

        ArrayList<User> users = new ArrayList<User>();
        if (cursor.getCount() > 0){
            cursor.moveToFirst();
            while (!cursor.isAfterLast()){
                boolean found = false;
                for (User u : users){
                    if (u.id == cursor.getLong(cursor.getColumnIndex(Users.ALIAS_ID))){
                        found = true;
                        break;
                    }
                }
                if (!found) users.add(new User(cursor,true));
                cursor.moveToNext();
            }
        }
        return users.toArray(new User[users.size()]);


    }

    public static void deleteActivitiesBefore(ContentResolver contentResolver, Long userId, long lastId, boolean exclusive) {
        Log.i(TAG, "Deleting rows  before " + lastId + " "
                        + +contentResolver.delete(Events.CONTENT_URI, Events.USER_ID + " = "
                                + userId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0") + " AND " + Events.ID + " <= " + lastId, null));

    }

    public static void writeTrack(ContentResolver contentResolver, Track track, WriteState writeState, Long currentUserId) {
            Cursor cursor = contentResolver.query(Content.TRACKS, new String[] {Tracks.ID}, Tracks.ID + " = ?",
                    new String[]{Long.toString(track.id)}, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
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

    public static int trimTracks(ContentResolver contentResolver, long[] currentPlaylist) {
        String[] whereArgs = new String[2];
        whereArgs[0] = whereArgs[1] = Boolean.toString(false);
        return contentResolver.delete(
                Content.TRACKS,
                "(user_favorite = 0 AND user_played = 0) AND id NOT IN ("
                        + joinArray(currentPlaylist, ",")
                        + ") AND id NOT IN (SELECT DISTINCT(origin_id) FROM Events)", null);
    }

    public static void writeUser(ContentResolver contentResolver,User user, WriteState writeState, Long currentUserId) {

        Cursor cursor = contentResolver.query(Content.USERS, new String[] {Users.ID}, Users.ID + " = ?",new String[]{Long.toString(user.id)}, null);

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

    public static String getUsernameById(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(Content.USERS, new String[] {Users.USERNAME}, Users.ID + "= ?",new String[]{Long.toString(userId)}, null);
        String user = "";
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = cursor.getString(0);
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


    public static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0) buf.append(delim);
            buf.append(list.get(i));
        }
        return buf.toString();
    }

    public static String joinArray(String[] list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.length;
        for (int i = 0; i < num; i++) {
            if (i != 0) buf.append(delim);
            buf.append(list[i]);
        }
        return buf.toString();
    }

    private static String joinArray(long[] list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.length;
        for (int i = 0; i < num; i++) {
            if (i != 0) buf.append(delim);
            buf.append(Long.toString(list[i]));
        }
        return buf.toString();
    }

}
