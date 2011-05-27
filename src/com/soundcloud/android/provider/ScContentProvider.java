package com.soundcloud.android.provider;

import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Content_Codes;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
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

import java.util.Arrays;

public class ScContentProvider extends ContentProvider {

    private static final String TAG = "ScContentProvider";
    public static final String AUTHORITY = "com.soundcloud.android.providers.ScContentProvider";
    private static final UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case Content_Codes.TRACKS:
                count = db.delete(Tables.TRACKS, where, whereArgs);
                break;
            case Content_Codes.USERS:
                count = db.delete(Tables.USERS, where, whereArgs);
                break;
            case Content_Codes.RECORDINGS:
                count = db.delete(Tables.RECORDINGS, where, whereArgs);
                break;
            case Content_Codes.EVENTS:
                count = db.delete(Tables.EVENTS, where, whereArgs);
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
            case Content_Codes.TRACKS:
                return Tracks.CONTENT_TYPE;
            case Content_Codes.USERS:
                return Users.CONTENT_TYPE;
            case Content_Codes.RECORDINGS:
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
            case Content_Codes.TRACKS:
                rowId = db.insert(Tables.TRACKS, Tracks.PERMALINK, values);
                if (rowId > 0) {
                    Uri trackUri = ContentUris.withAppendedId(Content.TRACKS, rowId);
                    getContext().getContentResolver().notifyChange(trackUri, null);
                    return trackUri;
                }
                break;
            case Content_Codes.USERS:
                rowId = db.insert(Tables.USERS, Users.PERMALINK, values);
                if (rowId > 0) {
                    Uri usersUri = ContentUris.withAppendedId(Content.USERS, rowId);
                    getContext().getContentResolver().notifyChange(usersUri, null);
                    return usersUri;
                }
                break;
            case Content_Codes.RECORDINGS:
                if (values.containsKey("_id")) values.remove("_id");
                rowId = db.insert(Tables.RECORDINGS, Recordings.AUDIO_PATH, values);
                if (rowId > 0) {
                    Uri recordingUri = ContentUris.withAppendedId(Content.RECORDINGS, rowId);
                    getContext().getContentResolver().notifyChange(recordingUri, null);
                    return recordingUri;
                }
                break;
            case Content_Codes.TRACK_PLAYS:
                if (values.containsKey("_id")) values.remove("_id");
                rowId = db.insert(Tables.TRACK_PLAYS, TrackPlays.TRACK_ID, values);
                if (rowId > 0) {
                    Uri trackPlaysUri = ContentUris.withAppendedId(Content.TRACK_PLAYS, rowId);
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
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        String table = "";

        if (selection == null) selection = "1";
        // XXX WTF
        switch (sUriMatcher.match(uri)) {
            case Content_Codes.TRACKS_ID:
                selection = selection + " AND " + Tracks.CONCRETE_ID + " = " + uri.getLastPathSegment();
            case Content_Codes.TRACKS:
                table = Tables.TRACKS;
                break;
            case Content_Codes.USERS_ID:
                selection = selection + " AND " + Users.ID + " = " + uri.getLastPathSegment();
            case Content_Codes.USERS:
                table = Tables.USERS;
                break;
            case Content_Codes.RECORDINGS_ID:
                selection = selection + " AND " + Recordings.ID + " = " + uri.getLastPathSegment();
            case Content_Codes.RECORDINGS:
                table = Tables.RECORDINGS;
                break;
            case Content_Codes.TRACK_PLAYS_ID:
                selection = selection + " AND " + Recordings.ID + " = " + uri.getLastPathSegment();
            case Content_Codes.TRACK_PLAYS:
                table = Tables.TRACK_PLAYS;
                break;
            case Content_Codes.EVENTS_ID:
                selection = selection + " AND " + Recordings.ID + " = " + uri.getLastPathSegment();
            case Content_Codes.EVENTS:
                table = Tables.EVENTS;
                break;
            case Content_Codes.EXCLUSIVE_TRACKS:
            case Content_Codes.INCOMING_TRACKS:
                table = Views.EVENTLIST_TRACK_ROW;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //Log.d(TAG, "query=("+table+","+(columns!=null? Arrays.asList(columns):null)+","+selection+","+
        //        (selectionArgs!=null? Arrays.asList(selectionArgs) : null)+")");

        Cursor c = db.query(table, columns, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case Content_Codes.TRACKS:
                count = db.update(Tables.TRACKS, values, where, whereArgs);
                break;
            case Content_Codes.USERS:
                count = db.update(Tables.USERS, values, where, whereArgs);
                break;
            case Content_Codes.RECORDINGS:
                count = db.update(Tables.RECORDINGS, values, where, whereArgs);
                break;
            case Content_Codes.TRACK_PLAYS:
                count = db.update(Tables.TRACK_PLAYS, values, where, whereArgs);
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
        sUriMatcher.addURI(AUTHORITY, Tables.TRACKS, Content_Codes.TRACKS);
        sUriMatcher.addURI(AUTHORITY, Tables.USERS, Content_Codes.USERS);
        sUriMatcher.addURI(AUTHORITY, Tables.RECORDINGS, Content_Codes.RECORDINGS);
        sUriMatcher.addURI(AUTHORITY, Tables.TRACK_PLAYS, Content_Codes.TRACK_PLAYS);
        sUriMatcher.addURI(AUTHORITY, Tables.EVENTS, Content_Codes.EVENTS);

        sUriMatcher.addURI(AUTHORITY, Tables.EVENTS+"/Incoming/Tracks", Content_Codes.INCOMING_TRACKS);
        sUriMatcher.addURI(AUTHORITY, Tables.EVENTS+"/Exclusive/Tracks", Content_Codes.EXCLUSIVE_TRACKS);

        sUriMatcher.addURI(AUTHORITY, Tables.TRACKS+"/#", Content_Codes.TRACKS_ID);
        sUriMatcher.addURI(AUTHORITY, Tables.USERS+"/#", Content_Codes.USERS_ID);
        sUriMatcher.addURI(AUTHORITY, Tables.RECORDINGS+"/#", Content_Codes.RECORDINGS_ID);
        sUriMatcher.addURI(AUTHORITY, Tables.TRACK_PLAYS+"/#", Content_Codes.TRACK_PLAYS_ID);
        sUriMatcher.addURI(AUTHORITY, Tables.EVENTS+"/#", Content_Codes.EVENTS);
    }



    private String getTableNameFromUri(Uri uri){
    switch (sUriMatcher.match(uri)) {
        case Content_Codes.TRACKS:
            return Tables.TRACKS;
        case Content_Codes.USERS:
            return Tables.USERS;
        case Content_Codes.RECORDINGS:
            return Tables.RECORDINGS;
        case Content_Codes.TRACK_PLAYS:
            return Tables.TRACK_PLAYS;
        case Content_Codes.EVENTS:
            return Tables.EVENTS;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
    }
}


}