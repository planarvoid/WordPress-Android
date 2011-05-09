package com.soundcloud.android.provider;

import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.provider.DatabaseHelper.Users;
import com.soundcloud.android.provider.DatabaseHelper.Views;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;

public class ScContentProvider extends ContentProvider {

    private static final String TAG = "ScContentProvider";
    public static final String AUTHORITY = "com.soundcloud.android.providers.ScContentProvider";
    private static final UriMatcher sUriMatcher;

    static final int TRACKS = 1;
    static final int USERS = 2;
    static final int RECORDINGS = 3;
    static final int TRACK_PLAYS = 4;

    static final int EVENTS = 10;
    static final int EVENTS_INCOMING_TRACKS = 11;
    static final int EVENTS_EXCLUSIVE_TRACKS = 12;

    static final int TRACKS_ID = 101;
    static final int USERS_ID = 102;
    static final int RECORDINGS_ID = 103;
    static final int TRACK_PLAYS_ID = 104;
    static final int EVENTS_ID = 105;

    static HashMap<String, String> tracksProjectionMap;
    static HashMap<String, String> usersProjectionMap;
    static HashMap<String, String> recordingsProjectionMap;
    static HashMap<String, String> trackPlaysProjectionMap;
    static HashMap<String, String> eventsProjectionMap;

    private static String COLUMN_ALL_FROM_TRACKS = "Tracks.*";
    private static String COLUMN_TRACK_USER_PLAYED = "CASE when TrackPlays.track_id is null then 0 else 1 END AS user_played";

    private static String COLUMN_ALL_FROM_USERS = "Users.*";
    private static String COLUMN_ALL_FROM_RECORDINGS = "Recordings.*";
    private static String COLUMN_ALL_FROM_EVENTS = "Events.*";

    public static String[] FULL_TRACK_PROJECTION = {COLUMN_ALL_FROM_TRACKS,COLUMN_TRACK_USER_PLAYED};
    public static String[] FULL_USER_PROJECTION = {COLUMN_ALL_FROM_USERS};
    public static String[] FULL_RECORDING_PROJECTION = {COLUMN_ALL_FROM_RECORDINGS};
    public static String[] FULL_EVENT_PROJECTION = {COLUMN_ALL_FROM_EVENTS};

    public static String[] FULL_EVENT_TRACK_PROJECTION = {COLUMN_ALL_FROM_EVENTS};

    public enum DbTable {
        Tracks(TRACKS,"Tracks",DatabaseHelper.DATABASE_CREATE_TRACKS,tracksProjectionMap),
        Users(USERS,"Users",DatabaseHelper.DATABASE_CREATE_USERS,usersProjectionMap),
        Recordings(RECORDINGS,"Recordings",DatabaseHelper.DATABASE_CREATE_RECORDINGS,recordingsProjectionMap),
        TrackPlays(TRACK_PLAYS,"TrackPlays",DatabaseHelper.DATABASE_CREATE_TRACK_PLAYS,trackPlaysProjectionMap),
        Events(EVENTS,"Events",DatabaseHelper.DATABASE_CREATE_EVENTS,eventsProjectionMap);

        public final int tblId;
        public final String tblName;
        public final String createString;
        public final HashMap<String,String> projectionMap;

        DbTable(int tblId, String tblName, String createString, HashMap<String,String> projectionMap) {
            this.tblId = tblId;
            this.tblName = tblName;
            this.createString = createString;
            this.projectionMap  = projectionMap;
        }
    }


    private DatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TRACKS:
                count = db.delete(DbTable.Tracks.tblName, where, whereArgs);
                break;
            case USERS:
                count = db.delete(DbTable.Users.tblName, where, whereArgs);
                break;
            case RECORDINGS:
                count = db.delete(DbTable.Recordings.tblName, where, whereArgs);
                break;
            case EVENTS:
                count = db.delete(DbTable.Events.tblName, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TRACKS:
                return Tracks.CONTENT_TYPE;
            case USERS:
                return Users.CONTENT_TYPE;
            case RECORDINGS:
                return Recordings.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId;
        switch (sUriMatcher.match(uri)) {
            case TRACKS:
                rowId = db.insert(DbTable.Tracks.tblName, Tracks.PERMALINK, values);
                if (rowId > 0) {
                    Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(trackUri, null);
                    return trackUri;
                }
                break;
            case USERS:
                rowId = db.insert(DbTable.Users.tblName, Users.PERMALINK, values);
                if (rowId > 0) {
                    Uri usersUri = ContentUris.withAppendedId(Users.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(usersUri, null);
                    return usersUri;
                }
                break;
            case RECORDINGS:
                if (values.containsKey("_id")) values.remove("_id");
                rowId = db.insert(DbTable.Recordings.tblName, Recordings.AUDIO_PATH, values);
                if (rowId > 0) {
                    Uri recordingUri = ContentUris.withAppendedId(Recordings.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(recordingUri, null);
                    return recordingUri;
                }
                break;
            case TRACK_PLAYS:
                if (values.containsKey("_id")) values.remove("_id");
                rowId = db.insert(DbTable.TrackPlays.tblName, TrackPlays.TRACK_ID, values);
                if (rowId > 0) {
                    Uri trackPlaysUri = ContentUris.withAppendedId(TrackPlays.CONTENT_URI, rowId);
                    getContext().getContentResolver().notifyChange(trackPlaysUri, null);
                    // TODO notify track of change too
                    return trackPlaysUri;
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case TRACKS_ID:
                qb.appendWhere(
                        Tracks.CONCRETE_ID + " = " + uri.getPathSegments().get(uri.getPathSegments().size() - 1));
            case TRACKS:
                //if (projection == null) projection = FULL_TRACK_PROJECTION;
                qb.setTables(Views.TRACKLIST_ROW);
                //qb.setProjectionMap(tracksProjectionMap);
                break;
            case USERS_ID:
                qb.appendWhere(
                        Users.ID + " = " + uri.getPathSegments().get(uri.getPathSegments().size() - 1));
            case USERS:
                if (projection == null) projection = FULL_USER_PROJECTION;
                qb.setTables(DbTable.Users.tblName);
                qb.setProjectionMap(usersProjectionMap);
                break;
            case RECORDINGS_ID:
                qb.appendWhere(
                        Recordings.ID + " = " + uri.getPathSegments().get(uri.getPathSegments().size() - 1));
            case RECORDINGS:
                if (projection == null) projection = FULL_RECORDING_PROJECTION;
                qb.setTables(DbTable.Recordings.tblName);
                qb.setProjectionMap(recordingsProjectionMap);
                break;
            case TRACK_PLAYS:
                qb.setTables(DbTable.TrackPlays.tblName);
                qb.setProjectionMap(trackPlaysProjectionMap);
                break;
            case EVENTS:
                if (projection == null) projection = FULL_EVENT_PROJECTION;
                qb.setTables(DbTable.Events.tblName);
                qb.setProjectionMap(eventsProjectionMap);
                break;
            case EVENTS_EXCLUSIVE_TRACKS:
            case EVENTS_INCOMING_TRACKS:
                //if (projection == null) projection = FULL_TRACK_PROJECTION;
                Log.i(TAG,"QQQUERYING THAT THINGA " + selection);
                qb.setTables(Views.EVENTLIST_TRACK_ROW);
                //qb.setProjectionMap(tracksProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TRACKS:
                count = db.update(DbTable.Tracks.tblName, values, where, whereArgs);
                break;
            case USERS:
                count = db.update(DbTable.Users.tblName, values, where, whereArgs);
                break;
            case RECORDINGS:
                count = db.update(DbTable.Recordings.tblName, values, where, whereArgs);
                break;
            case TRACK_PLAYS:
                count = db.update(DbTable.TrackPlays.tblName, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        String tblName = getTableNameFromUri(uri);
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (db.replace(tblName, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DbTable.Tracks.tblName, TRACKS);
        sUriMatcher.addURI(AUTHORITY, DbTable.Users.tblName, USERS);
        sUriMatcher.addURI(AUTHORITY, DbTable.Recordings.tblName, RECORDINGS);
        sUriMatcher.addURI(AUTHORITY, DbTable.TrackPlays.tblName, TRACK_PLAYS);
        sUriMatcher.addURI(AUTHORITY, DbTable.Events.tblName, EVENTS);

        sUriMatcher.addURI(AUTHORITY, DbTable.Events.tblName+"/Incoming/Tracks", EVENTS_INCOMING_TRACKS);
        sUriMatcher.addURI(AUTHORITY, DbTable.Events.tblName+"/Exclusive/Tracks", EVENTS_EXCLUSIVE_TRACKS);

        sUriMatcher.addURI(AUTHORITY, DbTable.Tracks.tblName+"/#", TRACKS_ID);
        sUriMatcher.addURI(AUTHORITY, DbTable.Users.tblName+"/#", USERS_ID);
        sUriMatcher.addURI(AUTHORITY, DbTable.Recordings.tblName+"/#", RECORDINGS_ID);
        sUriMatcher.addURI(AUTHORITY, DbTable.TrackPlays.tblName+"/#", TRACK_PLAYS_ID);
        sUriMatcher.addURI(AUTHORITY, DbTable.Events.tblName+"/#", EVENTS);

        tracksProjectionMap = new HashMap<String, String>();
        tracksProjectionMap.put(COLUMN_ALL_FROM_TRACKS, COLUMN_ALL_FROM_TRACKS);
        tracksProjectionMap.put(COLUMN_TRACK_USER_PLAYED, COLUMN_TRACK_USER_PLAYED);

        tracksProjectionMap.put(Tracks.ID, Tracks.ID);
        tracksProjectionMap.put(Tracks.PERMALINK, Tracks.PERMALINK);
        tracksProjectionMap.put(Tracks.DURATION, Tracks.DURATION);
        tracksProjectionMap.put(Tracks.TAG_LIST, Tracks.TAG_LIST);
        tracksProjectionMap.put(Tracks.CREATED_AT, Tracks.CREATED_AT);
        tracksProjectionMap.put(Tracks.TRACK_TYPE, Tracks.TRACK_TYPE);
        tracksProjectionMap.put(Tracks.TITLE, Tracks.TITLE);
        tracksProjectionMap.put(Tracks.PERMALINK_URL, Tracks.PERMALINK_URL);
        tracksProjectionMap.put(Tracks.ARTWORK_URL, Tracks.ARTWORK_URL);
        tracksProjectionMap.put(Tracks.WAVEFORM_URL, Tracks.WAVEFORM_URL);
        tracksProjectionMap.put(Tracks.DOWNLOADABLE, Tracks.DOWNLOADABLE);
        tracksProjectionMap.put(Tracks.DOWNLOAD_URL, Tracks.DOWNLOAD_URL);
        tracksProjectionMap.put(Tracks.STREAM_URL, Tracks.STREAM_URL);
        tracksProjectionMap.put(Tracks.STREAMABLE, Tracks.STREAMABLE);
        tracksProjectionMap.put(Tracks.SHARING, Tracks.SHARING);
        tracksProjectionMap.put(Tracks.USER_ID, Tracks.USER_ID);
        tracksProjectionMap.put(Tracks.USER_FAVORITE, Tracks.USER_FAVORITE);
        tracksProjectionMap.put(Tracks.FILELENGTH, Tracks.FILELENGTH);

        usersProjectionMap = new HashMap<String, String>();
        usersProjectionMap.put(COLUMN_ALL_FROM_USERS, COLUMN_ALL_FROM_USERS);
        usersProjectionMap.put(Users.ID, Users.ID);
        usersProjectionMap.put(Users.PERMALINK, Users.PERMALINK);
        usersProjectionMap.put(Users.AVATAR_URL, Users.AVATAR_URL);
        usersProjectionMap.put(Users.CITY, Users.CITY);
        usersProjectionMap.put(Users.COUNTRY, Users.COUNTRY);
        usersProjectionMap.put(Users.DISCOGS_NAME, Users.DISCOGS_NAME);
        usersProjectionMap.put(Users.FOLLOWERS_COUNT, Users.FOLLOWERS_COUNT);
        usersProjectionMap.put(Users.FOLLOWINGS_COUNT, Users.FOLLOWINGS_COUNT);
        usersProjectionMap.put(Users.FULL_NAME, Users.FULL_NAME);
        usersProjectionMap.put(Users.MYSPACE_NAME, Users.MYSPACE_NAME);
        usersProjectionMap.put(Users.TRACK_COUNT, Users.TRACK_COUNT);
        usersProjectionMap.put(Users.WEBSITE, Users.WEBSITE);
        usersProjectionMap.put(Users.WEBSITE_TITLE, Users.WEBSITE_TITLE);
        usersProjectionMap.put(Users.DESCRIPTION, Users.DESCRIPTION);

        recordingsProjectionMap = new HashMap<String, String>();
        recordingsProjectionMap.put(COLUMN_ALL_FROM_RECORDINGS, COLUMN_ALL_FROM_RECORDINGS);
        recordingsProjectionMap.put(Recordings.ID, Recordings.ID);
        recordingsProjectionMap.put(Recordings.USER_ID, Recordings.USER_ID);
        recordingsProjectionMap.put(Recordings.TIMESTAMP, Recordings.TIMESTAMP);
        recordingsProjectionMap.put(Recordings.LONGITUDE, Recordings.LONGITUDE);
        recordingsProjectionMap.put(Recordings.LATITUDE, Recordings.LATITUDE);
        recordingsProjectionMap.put(Recordings.WHAT_TEXT, Recordings.WHAT_TEXT);
        recordingsProjectionMap.put(Recordings.WHERE_TEXT, Recordings.WHERE_TEXT);
        recordingsProjectionMap.put(Recordings.AUDIO_PATH, Recordings.AUDIO_PATH);
        recordingsProjectionMap.put(Recordings.DURATION, Recordings.DURATION);
        recordingsProjectionMap.put(Recordings.ARTWORK_PATH, Recordings.ARTWORK_PATH);
        recordingsProjectionMap.put(Recordings.FOUR_SQUARE_VENUE_ID, Recordings.FOUR_SQUARE_VENUE_ID);
        recordingsProjectionMap.put(Recordings.SHARED_EMAILS, Recordings.SHARED_EMAILS);
        recordingsProjectionMap.put(Recordings.SERVICE_IDS, Recordings.SERVICE_IDS);
        recordingsProjectionMap.put(Recordings.IS_PRIVATE, Recordings.IS_PRIVATE);
        recordingsProjectionMap.put(Recordings.EXTERNAL_UPLOAD, Recordings.EXTERNAL_UPLOAD);
        recordingsProjectionMap.put(Recordings.AUDIO_PROFILE, Recordings.AUDIO_PROFILE);
        recordingsProjectionMap.put(Recordings.UPLOAD_STATUS, Recordings.UPLOAD_STATUS);
        recordingsProjectionMap.put(Recordings.UPLOAD_ERROR, Recordings.UPLOAD_ERROR);

        trackPlaysProjectionMap = new HashMap<String, String>();
        trackPlaysProjectionMap.put(TrackPlays.ID, TrackPlays.ID);
        trackPlaysProjectionMap.put(TrackPlays.TRACK_ID, TrackPlays.TRACK_ID);
        trackPlaysProjectionMap.put(TrackPlays.USER_ID, TrackPlays.USER_ID);

        eventsProjectionMap = new HashMap<String, String>();
        eventsProjectionMap.put(Events.ID, Events.ID);
        eventsProjectionMap.put(Events.BELONGS_TO_USER, Events.BELONGS_TO_USER);
        eventsProjectionMap.put(Events.TYPE, Events.TYPE);
        eventsProjectionMap.put(Events.CREATED_AT, Events.CREATED_AT);
        eventsProjectionMap.put(Events.TAGS, Events.TAGS);
        eventsProjectionMap.put(Events.ORIGIN_ID, Events.ORIGIN_ID);
        eventsProjectionMap.put(Events.LABEL, Events.LABEL);
        eventsProjectionMap.put("count("+Events.ID+")", "count("+Events.ID+")");

    }



    private String getTableNameFromUri(Uri uri){
    switch (sUriMatcher.match(uri)) {
        case TRACKS:
            return DbTable.Tracks.tblName;
        case USERS:
            return DbTable.Users.tblName;
        case RECORDINGS:
            return DbTable.Recordings.tblName;
        case TRACK_PLAYS:
            return DbTable.TrackPlays.tblName;
        case EVENTS:
            return DbTable.Events.tblName;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
    }
}


}