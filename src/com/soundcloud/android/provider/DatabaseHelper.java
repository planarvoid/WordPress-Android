package com.soundcloud.android.provider;

import com.soundcloud.android.provider.ScContentProvider.DbTable;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Database helper class for {@link SettingsProvider}.
 * Mostly just has a bit {@link #onCreate} to initialize the database.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ScContentProvider";
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 6;

    public interface Tables {
        public static final String EVENTS = "Events";
        public static final String TRACKS = "Tracks";
        public static final String USERS = "Users";
        public static final String TRACK_PLAYS = "TrackPlays";
        public static final String RECORDINGS = "Recordings";
    }

    public static final class Tracks implements BaseColumns {
        private Tracks() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Tracks");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.tracks";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.tracks";

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

        public static final String CONCRETE_ID = Tables.TRACKS + "." + ID;
        public static final String CONCRETE_PERMALINK = Tables.TRACKS + "." + PERMALINK;
        public static final String CONCRETE_CREATED_AT = Tables.TRACKS + "." + CREATED_AT;
        public static final String CONCRETE_DURATION = Tables.TRACKS + "." + DURATION;
        public static final String CONCRETE_TAG_LIST = Tables.TRACKS + "." + TAG_LIST;
        public static final String CONCRETE_TRACK_TYPE = Tables.TRACKS + "." + TRACK_TYPE;
        public static final String CONCRETE_TITLE = Tables.TRACKS + "." + TITLE;
        public static final String CONCRETE_PERMALINK_URL = Tables.TRACKS + "." + PERMALINK_URL;
        public static final String CONCRETE_ARTWORK_URL = Tables.TRACKS + "." + ARTWORK_URL;
        public static final String CONCRETE_WAVEFORM_URL = Tables.TRACKS + "." + WAVEFORM_URL;
        public static final String CONCRETE_DOWNLOADABLE = Tables.TRACKS + "." + DOWNLOADABLE;
        public static final String CONCRETE_DOWNLOAD_URL = Tables.TRACKS + "." + DOWNLOAD_URL;
        public static final String CONCRETE_STREAM_URL = Tables.TRACKS + "." + STREAM_URL;
        public static final String CONCRETE_STREAMABLE = Tables.TRACKS + "." + STREAMABLE;
        public static final String CONCRETE_SHARING = Tables.TRACKS + "." + SHARING;
        public static final String CONCRETE_USER_ID = Tables.TRACKS + "." + USER_ID;
        public static final String CONCRETE_USER_FAVORITE = Tables.TRACKS + "." + USER_FAVORITE;
        public static final String CONCRETE_USER_PLAYED = Tables.TRACKS + "." + USER_PLAYED;
        public static final String CONCRETE_FILELENGTH = Tables.TRACKS + "." + FILELENGTH;
    }

    public static final class TrackPlays implements BaseColumns {
        private TrackPlays() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/TrackPlays");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.track_plays";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.track_plays";

        public static final String ID = "_id";
        public static final String TRACK_ID = "track_id";
        public static final String USER_ID = "user_id";

        public static final String CONCRETE_ID = Tables.TRACK_PLAYS + "." + _ID;
        public static final String CONCRETE_TRACK_ID = Tables.TRACK_PLAYS + "." + TRACK_ID;
        public static final String CONCRETE_USER_ID = Tables.TRACK_PLAYS + "." + USER_ID;
    }

    public static final class Users implements BaseColumns {

        private Users() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Users");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.users";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.users";

        public static final String ID = "_id";
        public static final String USERNAME = "username";
        public static final String PERMALINK = "permalink";
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

        public static final String CONCRETE_ID = Tables.USERS + "." + _ID;
        public static final String CONCRETE_PERMALINK = Tables.USERS + "." + PERMALINK;
        public static final String CONCRETE_AVATAR_URL = Tables.USERS + "." + AVATAR_URL;
        public static final String CONCRETE_CITY = Tables.USERS + "." + CITY;
        public static final String CONCRETE_COUNTRY = Tables.USERS + "." + COUNTRY;
        public static final String CONCRETE_DISCOGS_NAME = Tables.USERS + "." + DISCOGS_NAME;
        public static final String CONCRETE_FOLLOWERS_COUNT = Tables.USERS + "." + FOLLOWERS_COUNT;
        public static final String CONCRETE_FOLLOWINGS_COUNT = Tables.USERS + "." + FOLLOWINGS_COUNT;
        public static final String CONCRETE_FULL_NAME = Tables.USERS + "." + FULL_NAME;
        public static final String CONCRETE_MYSPACE_NAME = Tables.USERS + "." + MYSPACE_NAME;
        public static final String CONCRETE_TRACK_COUNT = Tables.USERS + "." + TRACK_COUNT;
        public static final String CONCRETE_WEBSITE = Tables.USERS + "." + WEBSITE;
        public static final String CONCRETE_WEBSITE_TITLE = Tables.USERS + "." + WEBSITE_TITLE;
        public static final String CONCRETE_DESCRIPTION = Tables.USERS + "." + DESCRIPTION;
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

        public static final String CONCRETE_ID = Tables.RECORDINGS + "." + ID;
        public static final String CONCRETE_USER_ID = Tables.RECORDINGS + "." + USER_ID;
        public static final String CONCRETE_TIMESTAMP = Tables.RECORDINGS + "." + TIMESTAMP;
        public static final String CONCRETE_LONGITUDE = Tables.RECORDINGS + "." + LONGITUDE;
        public static final String CONCRETE_LATITUDE = Tables.RECORDINGS + "." + LATITUDE;
        public static final String CONCRETE_WHAT_TEXT = Tables.RECORDINGS + "." + WHAT_TEXT;
        public static final String CONCRETE_WHERE_TEXT = Tables.RECORDINGS + "." + WHERE_TEXT;
        public static final String CONCRETE_AUDIO_PATH = Tables.RECORDINGS + "." + AUDIO_PATH;
        public static final String CONCRETE_DURATION = Tables.RECORDINGS + "." + DURATION;
        public static final String CONCRETE_ARTWORK_PATH = Tables.RECORDINGS + "." + ARTWORK_PATH;
        public static final String CONCRETE_FOUR_SQUARE_VENUE_ID = Tables.RECORDINGS + "." + FOUR_SQUARE_VENUE_ID;
        public static final String CONCRETE_SHARED_EMAILS = Tables.RECORDINGS + "." + SHARED_EMAILS;
        public static final String CONCRETE_SERVICE_IDS = Tables.RECORDINGS + "." + SERVICE_IDS;
        public static final String CONCRETE_IS_PRIVATE = Tables.RECORDINGS + "." + IS_PRIVATE;
        public static final String CONCRETE_EXTERNAL_UPLOAD = Tables.RECORDINGS + "." + EXTERNAL_UPLOAD;
        public static final String CONCRETE_AUDIO_PROFILE = Tables.RECORDINGS + "." + AUDIO_PROFILE;
        public static final String CONCRETE_UPLOAD_STATUS = Tables.RECORDINGS + "." + UPLOAD_STATUS;
        public static final String CONCRETE_UPLOAD_ERROR = Tables.RECORDINGS + "." + UPLOAD_ERROR;
    }

    public static final class Events implements BaseColumns {
        private Events() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Events");

        public static final Uri CONTENT_INCOMING_TRACKS_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Events/Incoming/Tracks");

        public static final Uri CONTENT_EXCLUSIVE_TRACKS_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Events/Incoming/Tracks");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.events";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.events";

        public static final String ID = _ID;
        public static final String USER_ID = "user_id";
        public static final String TYPE = "type";
        public static final String CREATED_AT = "created_at";
        public static final String EXCLUSIVE = "exclusive";
        public static final String TAGS = "tags";
        public static final String LABEL = "label";
        public static final String ORIGIN_ID = "origin_id";
        public static final String NEXT_CURSOR = "next_cursor";

        public static final String CONCRETE_ID = Tables.EVENTS + "." + ID;
        public static final String CONCRETE_USER_ID = Tables.EVENTS + "." + USER_ID;
        public static final String CONCRETE_TYPE = Tables.EVENTS + "." + TYPE;
        public static final String CONCRETE_CREATED_AT = Tables.EVENTS + "." + CREATED_AT;
        public static final String CONCRETE_EXCLUSIVE = Tables.EVENTS + "." + EXCLUSIVE;
        public static final String CONCRETE_TAGS = Tables.EVENTS + "." + TAGS;
        public static final String CONCRETE_LABEL = Tables.EVENTS + "." + LABEL;
        public static final String CONCRETE_ORIGIN_ID = Tables.EVENTS + "." + ORIGIN_ID;
        public static final String CONCRETE_NEXT_CURSOR = Tables.EVENTS + "." + NEXT_CURSOR;
    }

    DatabaseHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(DATABASE_CREATE_EVENTS);
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
            db.execSQL(DATABASE_CREATE_USERS);
            db.execSQL(DATABASE_CREATE_RECORDINGS);
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
                        case 6:
                            success = upgradeTo6(db);
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
                Log.i(TAG,"UPGRADE NOT SUCCESSFULL");
                /*db.execSQL("DROP TABLE IF EXISTS Tracks");
                db.execSQL("DROP TABLE IF EXISTS Users");
                db.execSQL("DROP TABLE IF EXISTS Recordings");
                db.execSQL("DROP TABLE IF EXISTS TrackPlays");
                onCreate(db);*/
            }
            db.endTransaction();
        } else {
            db.execSQL("DROP TABLE IF EXISTS Tracks");
            db.execSQL("DROP TABLE IF EXISTS Users");
            db.execSQL("DROP TABLE IF EXISTS Recordings");
            db.execSQL("DROP TABLE IF EXISTS TrackPlays");
            onCreate(db);
        }

    }

        /*
         * altered id naming for content resolver
         */
        private boolean upgradeTo4(SQLiteDatabase db) {
            try {
                alterTableColumns(db, DbTable.Tracks, new String[] {
                    "id"
                }, new String[] {
                    "_id"
                });
                alterTableColumns(db, DbTable.Users, new String[] {
                    "id"
                }, new String[] {
                    "_id"
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

        /*
         * added sharing to database
         */
        private boolean upgradeTo6(SQLiteDatabase db) {
            try {
                db.execSQL(DATABASE_CREATE_EVENTS);
                db.execSQL(DATABASE_CREATE_RECORDINGS);
                db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
                alterTableColumns(db, DbTable.Tracks, null, null);
                alterTableColumns(db, DbTable.Users, null, null);

                createTrackViews(db);

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
            List<String> columns = getColumnNames(db, "bck_" + tbl.tblName);
            columns.retainAll(getColumnNames(db, tbl.tblName));
            String cols = TextUtils.join(",",columns);
            String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                    + TextUtils.join(",",toAppendCols) : cols;
            String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                    + TextUtils.join(",",fromAppendCols) : cols;
            db.execSQL(String.format("INSERT INTO bck_%s (%s) SELECT %s from %s", tbl.tblName,
                    toCols, fromCols, tbl.tblName));
            db.execSQL("DROP table  '" + tbl.tblName + "'");
            db.execSQL(tbl.createString);
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from bck_%s", tbl.tblName,
                    cols, cols, tbl.tblName));
            db.execSQL("DROP table bck_" + tbl.tblName);
            return cols;
        }


        private static void createTrackViews(SQLiteDatabase db) {

        }
        private static void createEventViews(SQLiteDatabase db) {

        }

    public static boolean isValidTable(String name) {
        return mValidTables.contains(name);
    }



    public static List<String> getColumnNames(SQLiteDatabase db, String tableName){
        Cursor ti = db.rawQuery("pragma table_info ("+tableName+")",null);
        if ( ti.moveToFirst() ) {
            ArrayList<String> cols = new ArrayList<String>();
            int i = 0;
            do {
                cols.add(ti.getString(1));
                i++;
            } while (ti.moveToNext());
            return cols;
        }
        return null;
    }


    static final String DATABASE_CREATE_EVENTS = "create table Events (_id integer primary key AUTOINCREMENT, "
        + "created_at integer null, "
        + "user_id integer null, "
        + "type string null, "
        + "exclusive boolean false, "
        + "origin_id integer null, "
        + "tags string null, "
        + "label string null, "
        + "next_cursor string null);";


    static final String DATABASE_CREATE_TRACKS = "create table Tracks (_id integer primary key, "
            + "permalink string null, "
            + "duration integer null, "
            + "created_at integer null, "
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
            + "user_id integer null, "
            + "user_favorite boolean false, "
            + "filelength integer null);";

    static final String DATABASE_CREATE_TRACK_PLAYS = "create table TrackPlays (_id integer primary key AUTOINCREMENT, "
            + "track_id integer null, " + "user_id integer null);";

    static final String DATABASE_CREATE_USERS = "create table Users (_id integer primary key, "
            + "username string null, "
            + "avatar_url string null, "
            + "permalink string null, "
            + "city string null, "
            + "country string null, "
            + "discogs_name string null, "
            + "followers_count integer null, "
            + "followings_count integer null, "
            + "full_name string null, "
            + "myspace_name string null, "
            + "track_count integer null, "
            + "website string null, "
            + "website_title string null, " + "description text null);";

    static final String DATABASE_CREATE_RECORDINGS = "create table Recordings (_id integer primary key AUTOINCREMENT, "
            + "user_id integer null, "
            + "timestamp integer null, "
            + "longitude string null, "
            + "latitude string null, "
            + "what_text string null, "
            + "where_text string null, "
            + "audio_path string null, "
            + "artwork_path string null, "
            + "duration integer null, "
            + "four_square_venue_id string null, "
            + "shared_emails text null, "
            + "service_ids string null, "
            + "is_private boolean false, "
            + "external_upload boolean false, "
            + "audio_profile integer null, "
            + "upload_status integer false, " + "upload_error boolean false);";

    static final HashSet<String> mValidTables = new HashSet<String>();
    static {
        mValidTables.add(Tables.TRACKS);
        mValidTables.add(Tables.TRACK_PLAYS);
        mValidTables.add(Tables.RECORDINGS);
        mValidTables.add(Tables.USERS);
        mValidTables.add(Tables.EVENTS);
    }
}
