package com.soundcloud.android.provider;

import com.soundcloud.android.objects.Track.Tracks;
import com.soundcloud.android.objects.User.Users;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ScContentProvider extends ContentProvider {

    private static final String TAG = "ScContentProvider";

    private static final String DATABASE_NAME = "SoundCloud";

    private static final int DATABASE_VERSION = 5;

    public static final String AUTHORITY = "com.soundcloud.android.providers.ScContentProvider";

    private static final UriMatcher sUriMatcher;

    private static final int TRACKS = 1;

    private static final int USERS = 2;

    private static HashMap<String, String> tracksProjectionMap;

    private static HashMap<String, String> usersProjectionMap;

    public enum DbTable {
        Tracks(TRACKS,"Tracks",DATABASE_CREATE_TRACKS,tracksProjectionMap),
        Users(USERS,"Users",DATABASE_CREATE_USERS,usersProjectionMap);

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

    private static final String DATABASE_CREATE_TRACKS = "create table Tracks (_id string primary key, "
        + "permalink string null, "
        + "duration string null, "
        + "tag_list string null, "
        + "track_type string null, "
        + "title string null, "
        + "permalink_url string null, "
        + "artwork_url string null, "
        + "waveform_url string null, "
        + "downloadable string null, "
        + "download_url string null, "
        + "stream_url string null, "
        + "streamable string null, "
        + "sharing string null, "
        + "user_id string null, "
        + "user_favorite boolean false, "
        + "user_played boolean false, "
        + "filelength integer null);";

private static final String DATABASE_CREATE_USERS = "create table Users (_id string primary key, "
        + "username string null, "
        + "avatar_url string null, "
        + "permalink string null, "
        + "city string null, "
        + "country string null, "
        + "discogs_name string null, "
        + "followers_count string null, "
        + "followings_count string null, "
        + "full_name string null, "
        + "myspace_name string null, "
        + "track_count string null, "
        + "website string null, "
        + "website_title string null, "
        + "description text null);";

private static class DatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;

    DatabaseHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_USERS);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion +
         " to " + newVersion);

        if (newVersion > oldVersion) {
            db.beginTransaction();

            boolean success = false;
            if (oldVersion >= 3){
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 4:
                            success = upgradeTo4(db);
                            break;
                        case 5:
                            success = upgradeTo5(db);
                            break;
                        default:
                            break;
                    }

                    if (!success) {
                        break;
                    }
                }

            }

            if (success) {
                Log.i(TAG,"SUCCESSFUL UPGRADE");
                db.setTransactionSuccessful();
            } else {
                db.execSQL("DROP TABLE IF EXISTS Tracks");
                db.execSQL("DROP TABLE IF EXISTS Users");
                db.execSQL("DROP TABLE IF EXISTS Followings");
                db.execSQL("DROP TABLE IF EXISTS Favorites");
                onCreate(db);
            }
            db.endTransaction();
        } else {
            db.execSQL("DROP TABLE IF EXISTS Tracks");
            db.execSQL("DROP TABLE IF EXISTS Users");
            db.execSQL("DROP TABLE IF EXISTS Followings");
            db.execSQL("DROP TABLE IF EXISTS Favorites");
            onCreate(db);
        }

    }

        /*
         * altered id naming for content resolver
         */
        private boolean upgradeTo4(SQLiteDatabase db) {
            try {
                alterTableColumns(db, DbTable.Tracks, new String[] {
                    ",id"
                }, new String[] {
                    ",_id"
                });
                alterTableColumns(db, DbTable.Users, new String[] {
                    ",id"
                }, new String[] {
                    ",_id"
                });
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /*
         * added sharing to database
         */
        private boolean upgradeTo5(SQLiteDatabase db) {
            try {
                alterTableColumns(db, DbTable.Tracks, null, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private String alterTableColumns(SQLiteDatabase db, DbTable tbl, String[] fromAppendCols,
                String[] toAppendCols) {
            db.execSQL("DROP TABLE IF EXISTS bck_" + tbl.tblName);
            db.execSQL(tbl.createString.replace("create table " + tbl.tblName, "create table bck_"
                    + tbl.tblName));
            List<String> columns = GetColumns(db, "bck_" + tbl.tblName);
            columns.retainAll(GetColumns(db, tbl.tblName));
            String cols = join(columns, ",");
            String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                    + joinArray(toAppendCols, ",") : cols;
            String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                    + joinArray(fromAppendCols, ",") : cols;
            db.execSQL(String.format("INSERT INTO bck_%s (%s) SELECT %s from %s", tbl.tblName,
                    toCols, fromCols, tbl.tblName));
            db.execSQL("DROP table  '" + tbl.tblName + "'");
            db.execSQL(tbl.createString);
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from bck_%s", tbl.tblName,
                    cols, cols, tbl.tblName));
            db.execSQL("DROP table bck_" + tbl.tblName);
            return cols;
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
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

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
            case TRACKS:
                qb.setTables(DbTable.Tracks.tblName);
                qb.setProjectionMap(tracksProjectionMap);
                break;
            case USERS:
                qb.setTables(DbTable.Users.tblName);
                qb.setProjectionMap(usersProjectionMap);
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

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DbTable.Tracks.tblName, TRACKS);
        sUriMatcher.addURI(AUTHORITY, DbTable.Users.tblName, USERS);

        tracksProjectionMap = new HashMap<String, String>();
        tracksProjectionMap.put(Tracks.ID, Tracks.ID);
        tracksProjectionMap.put(Tracks.PERMALINK, Tracks.PERMALINK);
        tracksProjectionMap.put(Tracks.DURATION, Tracks.DURATION);
        tracksProjectionMap.put(Tracks.TAG_LIST, Tracks.TAG_LIST);
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
        tracksProjectionMap.put(Tracks.USER_PLAYED, Tracks.USER_PLAYED);
        tracksProjectionMap.put(Tracks.FILELENGTH, Tracks.FILELENGTH);

        usersProjectionMap = new HashMap<String, String>();
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

    }

    public static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String[] GetColumnsArray(SQLiteDatabase db, String tableName) {
        String[] ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = c.getColumnNames();
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
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




}