package com.soundcloud.android;

import com.soundcloud.android.objects.Activities;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SoundCloudDB {
    private static final String TAG = "ScSyncAdapterService";

    private SoundCloudDB () {
    }

    public enum WriteState {
        none, insert_only, update_only, all
    }

    private static final SoundCloudDB instance = new SoundCloudDB();

    public static SoundCloudDB getInstance() {
        return instance;
    }

    public boolean updateActivities(SoundCloudApplication app, ContentResolver contentResolver,
            Long currentUserId, boolean exclusive) throws JsonParseException, JsonMappingException,
            IllegalStateException, IOException {

      app.setAccountData(User.DataKeys.LAST_INCOMING_SYNC,System.currentTimeMillis());

        // get the timestamp of the newest record in the database
        Cursor firstCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
                Events.ID, Events.ORIGIN_ID,
        }, Events.USER_ID + " = " + currentUserId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0"), null, Events.ID + " DESC LIMIT 1");

        if (firstCursor.getCount() > 0)
            firstCursor.moveToFirst();

        final long firstTrackId = firstCursor.getCount() == 0 ? 0 : firstCursor.getLong(1);
        firstCursor.close();

        int added = 0;
        boolean caughtUp = false;
        Activities activities = null;

        List<ContentValues> tracksCV = new ArrayList<ContentValues>();
        List<ContentValues> eventsCV = new ArrayList<ContentValues>();
        List<ContentValues> usersCV = new ArrayList<ContentValues>();

        do {
            Request request = Request.to(exclusive ? Endpoints.MY_EXCLUSIVE_TRACKS : Endpoints.MY_ACTIVITIES);
            request.add("limit", Integer.toString(20));

            if (activities != null) { // add next cursor if applicable
                List<NameValuePair> params = URLEncodedUtils.parse( URI.create(activities.next_href), "UTF-8");
                for (NameValuePair param : params) {
                    if (param.getName().equalsIgnoreCase("cursor")) {
                        request.add("cursor", param.getValue());
                    }
                }
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

                        // TODO do not always add every user and track. keep a map to check against
                        // what was already added, otherwise unnecessary db IO

                        tracksCV.add(0, evt.getTrack().buildContentValues());
                        usersCV.add(0, evt.getTrack().user.buildContentValues(false));
                        eventsCV.add(0, evt.buildContentValues(currentUserId, exclusive));


                    } else {
                        caughtUp = true;
                        break;
                    }
                }

                if (firstTrackId <= 0 && added > 100)
                    caughtUp = true;

                Log.i(TAG, "keeping " + added + " of " + activities.size() + " activities");
            } else {
                return false;
            }
        } while (!caughtUp);

        contentResolver.bulkInsert(Tracks.CONTENT_URI,
                tracksCV.toArray(new ContentValues[tracksCV.size()]));
        contentResolver.bulkInsert(Users.CONTENT_URI,
                usersCV.toArray(new ContentValues[usersCV.size()]));
        contentResolver.bulkInsert(Events.CONTENT_URI,
                eventsCV.toArray(new ContentValues[eventsCV.size()]));

        return true;

    }

     // ---Make sure the database is up to date with this track info---
        public void insertActivities(ContentResolver contentResolver, Activities activities, Long currentUserId) {
            insertActivities(contentResolver, activities, currentUserId, 0);
        }

        public int insertActivities(ContentResolver contentResolver, Activities activities,  Long currentUserId, long stopAtTrackId) {

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

            contentResolver.bulkInsert(Tracks.CONTENT_URI, tracksCV.toArray(new ContentValues[tracksCV.size()]));
            contentResolver.bulkInsert(Users.CONTENT_URI, usersCV.toArray(new ContentValues[usersCV.size()]));
            contentResolver.bulkInsert(Events.CONTENT_URI, eventsCV.toArray(new ContentValues[eventsCV.size()]));
            return inserted;
        }

    public void cleanStaleActivities(ContentResolver contentResolver, Long userId, int maxEvents, boolean exclusive) {
        Cursor countCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
            "count(" + Events.ID + ")",
        }, Events.USER_ID + " = " + userId, null, null);

        countCursor.moveToFirst();
        int eventsCount = countCursor.getInt(0);
        countCursor.close();

        Log.i(TAG,"Cleaning Stale Activities for user " + userId + ", deleting" + Math.max(0,(eventsCount - maxEvents)) + " of " + eventsCount);

        // if there are older entries, delete them as necessary
        if (eventsCount > maxEvents) {
            Cursor lastCursor = contentResolver.query(Events.CONTENT_URI, new String[] {
                Events.ID,
            }, Events.USER_ID + " = " + userId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0"), null, Events.ID + " DESC LIMIT "
                    + maxEvents);

            lastCursor.moveToLast();
            long lastId = lastCursor.getLong(0);

            Log.i(TAG,
                    "Deleting rows " + lastId + " "
                            + contentResolver.delete(Events.CONTENT_URI, Events.USER_ID + " = "
                                    + userId + " AND " + Events.ID + " < " + lastId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0"),
                                    null));
        }

    }

    public void deleteActivitiesBefore(ContentResolver contentResolver, Long userId, long lastId, boolean exclusive) {
        Log.i(TAG, "Deleting rows  before " + lastId + " "
                        + +contentResolver.delete(Events.CONTENT_URI, Events.USER_ID + " = "
                                + userId + " AND " + Events.EXCLUSIVE + " = " + (exclusive ? "1" : "0") + " AND " + Events.ID + " <= " + lastId, null));

    }



        // ---Make sure the database is up to date with this track info---
        public void resolveTrack(ContentResolver contentResolver, Track track, WriteState writeState, Long currentUserId) {

            synchronized(this){
                Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.FULL_ID + "='" + track.id + "'", null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {

                        cursor.moveToFirst();

                        // add non-remote variables and update database if necessary
                        track.user_played = track.user_played || cursor.getInt(cursor.getColumnIndex("user_played")) == 1;
                        track.filelength = track.filelength == 0 ? cursor.getInt(cursor.getColumnIndex("filelength")) : track.filelength;

                        if (writeState == WriteState.update_only || writeState == WriteState.all)
                            contentResolver.update(Tracks.CONTENT_URI, track.buildContentValues(), Tracks.ID + "='" + track.id + "'", null);

                    } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                        contentResolver.insert(Tracks.CONTENT_URI, track.buildContentValues());
                    }

                    cursor.close();

                    // write with insert only because a track will never come in with
                    resolveUser(contentResolver, track.user, WriteState.insert_only, currentUserId);
                }
            }
        }

        // ---Make sure the database is up to date with this track info---
        public Track resolveTrackById(ContentResolver contentResolver, long TrackId,
                long currentUserId) {

            Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.FULL_ID + "='" + TrackId + "'", null, null);
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                Track track = new Track(cursor);
                cursor.close();

                User user = resolveUserById(contentResolver, track.user_id);
                if (user != null){
                    track.user = user;
                    track.user_id = user.id;
                }


                return track;
            }

            cursor.close();
            return null;

        }


        public boolean isTrackInDb(ContentResolver contentResolver, long id) {
            boolean ret = false;
            Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.ID + "='" + id + "'", null, null);
            if (null != cursor && cursor.moveToNext()) {
                ret = true;
            }

            if (cursor != null) cursor.close();
            return ret;
        }

    public int trimTracks(ContentResolver contentResolver, long[] currentPlaylist) {
        String[] whereArgs = new String[2];
        whereArgs[0] = whereArgs[1] = Boolean.toString(false);
        return contentResolver.delete(
                Tracks.CONTENT_URI,
                "(user_favorite = 0 AND user_played = 0) AND id NOT IN ("
                        + joinArray(currentPlaylist, ",")
                        + ") AND id NOT IN (SELECT DISTINCT(origin_id) FROM Events)", null);
    }

        public void resolveUser(ContentResolver contentResolver,User user, WriteState writeState,
                Long currentUserId) {

            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + user.id + "'", null, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    user.update(cursor); // update the parcelable with values from the db

                    if (writeState == WriteState.update_only || writeState == WriteState.all)
                        contentResolver.update(Users.CONTENT_URI, user.buildContentValues(currentUserId.compareTo(user.id) == 0), Users.ID + "='" + user.id + "'", null);

                } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                    contentResolver.insert(Users.CONTENT_URI, user.buildContentValues(currentUserId.compareTo(user.id) == 0));
                }
                cursor.close();
            }
        }

        public User resolveUserById(ContentResolver contentResolver, long userId) {
            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + userId + "'", null, null);
            User user = null;
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                user = new User(cursor);
            }
            if (cursor != null) cursor.close();
            return user;
        }

        public boolean isUserInDb(ContentResolver contentResolver, long id) {
            boolean ret = false;
            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + id + "'", null, null);
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
                if (i != 0)
                    buf.append(delim);
                buf.append(list.get(i));
            }
            return buf.toString();
        }

        public static String joinArray(String[] list, String delim) {
            StringBuilder buf = new StringBuilder();
            int num = list.length;
            for (int i = 0; i < num; i++) {
                if (i != 0)
                    buf.append(delim);
                buf.append(list[i]);
            }
            return buf.toString();
        }

        private String joinArray(long[] list, String delim) {
            StringBuilder buf = new StringBuilder();
            int num = list.length;
            for (int i = 0; i < num; i++) {
                if (i != 0)
                    buf.append(delim);
                buf.append(Long.toString(list[i]));
            }
            return buf.toString();
        }

        public static final class Tracks implements BaseColumns {
            private Tracks() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + ScContentProvider.AUTHORITY + "/Tracks");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.tracks";
            public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.tracks";

            public static final String FULL_ID = "Tracks._id";

            public static final String ID = "_id";
            public static final String PERMALINK = "permalink";
            public static final String CREATED_AT = "created_at";
            public static final String DURATION = "duration";
            public static final String TAG_LIST = "tag_list";
            public static final String TRACK_TYPE = "track_type";
            public static final String TITLE = "title";
            public static final String PERMALINK_URL = "permalink_url";
            public static final String ARTWORK_URL = "artwork_url";
            public static final String WAVEFORM_URL = "waveform_url";
            public static final String DOWNLOADABLE = "downloadable";
            public static final String DOWNLOAD_URL = "download_url";
            public static final String STREAM_URL = "stream_url";
            public static final String STREAMABLE = "streamable";
            public static final String SHARING = "sharing";
            public static final String USER_ID = "user_id";
            public static final String USER_FAVORITE = "user_favorite";
            public static final String USER_PLAYED = "user_played";
            public static final String FILELENGTH = "filelength";
        }

        public static final class TrackPlays implements BaseColumns {
            private TrackPlays() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + ScContentProvider.AUTHORITY + "/TrackPlays");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.track_plays";
            public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.track_plays";

            public static final String FULL_ID = "TrackPlays._id";

            public static final String ID = "_id";
            public static final String TRACK_ID = "track_id";
            public static final String USER_ID = "user_id";
        }

        public static final class Users implements BaseColumns {

            private Users() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + ScContentProvider.AUTHORITY + "/Users");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.users";
            public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.users";
            public static final String ID = "_id";
            public static final String PERMALINK = "username";
            public static final String AVATAR_URL = "avatar_url";
            public static final String CITY = "city";
            public static final String COUNTRY = "country";
            public static final String DISCOGS_NAME = "discogs_name";
            public static final String FOLLOWERS_COUNT = "followers_count";
            public static final String FOLLOWINGS_COUNT = "followings_count";
            public static final String FULL_NAME = "full_name";
            public static final String MYSPACE_NAME = "myspace_name";
            public static final String TRACK_COUNT = "track_count";
            public static final String WEBSITE = "website";
            public static final String WEBSITE_TITLE = "website_title";
            public static final String DESCRIPTION = "description";
        }

        public static final class Recordings implements BaseColumns {
            private Recordings() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + ScContentProvider.AUTHORITY + "/Recordings");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.recordings";
            public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.recordings";

            public static final String ID = "_id";
            public static final String USER_ID = "user_id";
            public static final String TIMESTAMP = "timestamp";
            public static final String LONGITUDE = "longitude";
            public static final String LATITUDE = "latitude";
            public static final String WHAT_TEXT = "what_text";
            public static final String WHERE_TEXT = "where_text";
            public static final String AUDIO_PATH = "audio_path";
            public static final String DURATION = "duration";
            public static final String ARTWORK_PATH = "artwork_path";
            public static final String FOUR_SQUARE_VENUE_ID = "four_square_venue_id";
            public static final String SHARED_EMAILS = "shared_emails";
            public static final String SERVICE_IDS = "service_ids";
            public static final String IS_PRIVATE = "is_private";
            public static final String EXTERNAL_UPLOAD = "external_upload";
            public static final String AUDIO_PROFILE = "audio_profile";
            public static final String UPLOAD_STATUS = "upload_status";
            public static final String UPLOAD_ERROR = "upload_error";
        }

        public static final class Events implements BaseColumns {
            private Events() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + ScContentProvider.AUTHORITY + "/Events");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.events";
            public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.events";

            public static final String ID = "_id";
            public static final String USER_ID = "user_id";
            public static final String TYPE = "type";
            public static final String CREATED_AT = "created_at";
            public static final String EXCLUSIVE = "exclusive";
            public static final String TAGS = "tags";
            public static final String LABEL = "label";
            public static final String ORIGIN_ID = "origin_id";
            public static final String NEXT_CURSOR = "next_cursor";
        }





    }