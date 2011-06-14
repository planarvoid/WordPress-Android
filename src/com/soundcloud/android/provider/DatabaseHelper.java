package com.soundcloud.android.provider;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ScContentProvider";
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 6;


    public enum Tables {
        TRACKS("Tracks", DATABASE_CREATE_TRACKS),
        RECORDINGS("Recordings", DATABASE_CREATE_RECORDINGS),
        USERS("Users", DATABASE_CREATE_USERS),
        TRACK_PLAYS("TrackPlays", DATABASE_CREATE_TRACK_PLAYS),
        EVENTS("Events", DATABASE_CREATE_EVENTS);


        public final String tableName;
        public final Uri uri;
        public final String createString;


        Tables(String name, String create) {
            tableName = name;
            uri = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/"+tableName);
            createString = create;
        }

        public static Tables get(String name) {
            EnumSet<DatabaseHelper.Tables> tables = EnumSet.allOf(DatabaseHelper.Tables.class);
            for (Tables table : tables) {
                if (table.tableName.equals(name)) return table;
            }
            return null;
        }
    }

    public interface Content {
        Uri TRACKS  = Tables.TRACKS.uri;
        Uri USERS = Tables.USERS.uri;
        Uri RECORDINGS = Tables.RECORDINGS.uri;
        Uri TRACK_PLAYS = Tables.TRACK_PLAYS.uri;
        Uri EVENTS = Tables.EVENTS.uri;

        Uri INCOMING_TRACKS = Uri.parse("content://" + ScContentProvider.AUTHORITY + "/Events/Incoming/Tracks");
        Uri EXCLUSIVE_TRACKS = Uri.parse("content://" + ScContentProvider.AUTHORITY + "/Events/Incoming/Tracks");
    }


    DatabaseHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
            db.execSQL(DATABASE_CREATE_USERS);
            db.execSQL(DATABASE_CREATE_RECORDINGS);
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during onCreate()", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version "+oldVersion+" to "+newVersion);
        if (newVersion > oldVersion) {
            db.beginTransaction();
            boolean success = false;
            if (oldVersion >= 3) {
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 4:
                            success = upgradeTo4(db, oldVersion);
                            break;
                        case 5:
                            success = upgradeTo5(db, oldVersion);
                            break;
                        case 6:
                            success = upgradeTo6(db, oldVersion);
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
                Log.d(TAG, "successful upgrade");
            } else {
                Log.w(TAG,"upgrade not successful, recreating db");
                recreateDb(db);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        } else {
            recreateDb(db);
        }
    }

    private void recreateDb(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS Tracks");
        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Recordings");
        db.execSQL("DROP TABLE IF EXISTS TrackPlays");
        onCreate(db);
    }

    /*
     * altered id naming for content resolver
     */
    private boolean upgradeTo4(SQLiteDatabase db, int oldVersion) {
        try {
            alterTableColumns(db, Tables.TRACKS, new String[] {
                "id"
            }, new String[] {
                "_id"
            });
            alterTableColumns(db, Tables.USERS, new String[] {
                "id"
            }, new String[] {
                "_id"
            });
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade4 "+
                       "(from "+oldVersion+")", e);
        }
        return false;
    }

    /*
     * added sharing to database
     */
    private boolean upgradeTo5(SQLiteDatabase db, int oldVersion) {
            try {
                alterTableColumns(db, Tables.TRACKS, null, null);
                return true;
            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade5 " +
                        "(from "+oldVersion+")", e);
            }
            return false;
        }

    /*
     * added sharing to database
     */
    private boolean upgradeTo6(SQLiteDatabase db, int oldVersion) {
            try {
                db.execSQL(DATABASE_CREATE_RECORDINGS);
                db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
                alterTableColumns(db, Tables.TRACKS, null, null);
                alterTableColumns(db, Tables.USERS, null, null);
                return true;
            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade6 " +
                        "(from "+oldVersion+")", e);
            }
            return false;
        }

    private String alterTableColumns(SQLiteDatabase db, Tables tbl, String[] fromAppendCols,
                                     String[] toAppendCols) {

        db.execSQL("DROP TABLE IF EXISTS bck_" + tbl.tableName);
        db.execSQL(tbl.createString.replace("create table " + tbl.tableName, "create table bck_" + tbl.tableName));
        List<String> columns = getColumnNames(db, "bck_" + tbl.tableName);
        columns.retainAll(getColumnNames(db, tbl.tableName));
        String cols = TextUtils.join(",", columns);

        String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", toAppendCols) : cols;

        String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", fromAppendCols) : cols;

        db.execSQL(String.format("INSERT INTO bck_%s (%s) SELECT %s from %s",
                tbl.tableName, toCols, fromCols, tbl.tableName));

        db.execSQL("DROP table  '" + tbl.tableName + "'");
        db.execSQL(tbl.createString);
        db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from bck_%s",
                tbl.tableName, cols, cols, tbl.tableName));

        db.execSQL("DROP table bck_" + tbl.tableName);
        return cols;
    }


    public static List<String> getColumnNames(SQLiteDatabase db, String tableName){
        Cursor ti = db.rawQuery("pragma table_info ("+tableName+")", null);
        if (ti != null && ti.moveToFirst() ) {
            List<String> cols = new ArrayList<String>();
            do {
                cols.add(ti.getString(1));
            } while (ti.moveToNext());

            ti.close();
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


    public static final class Tracks implements BaseColumns {

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

          public static final String ALIAS_ID = Tables.TRACKS + "_" + ID;
          public static final String ALIAS_PERMALINK = Tables.TRACKS + "_" + PERMALINK;
          public static final String ALIAS_CREATED_AT = Tables.TRACKS + "_" + CREATED_AT;
          public static final String ALIAS_DURATION = Tables.TRACKS + "_" + DURATION;
          public static final String ALIAS_TAG_LIST = Tables.TRACKS + "_" + TAG_LIST;
          public static final String ALIAS_TRACK_TYPE = Tables.TRACKS + "_" + TRACK_TYPE;
          public static final String ALIAS_TITLE = Tables.TRACKS + "_" + TITLE;
          public static final String ALIAS_PERMALINK_URL = Tables.TRACKS + "_" + PERMALINK_URL;
          public static final String ALIAS_ARTWORK_URL = Tables.TRACKS + "_" + ARTWORK_URL;
          public static final String ALIAS_WAVEFORM_URL = Tables.TRACKS + "_" + WAVEFORM_URL;
          public static final String ALIAS_DOWNLOADABLE = Tables.TRACKS + "_" + DOWNLOADABLE;
          public static final String ALIAS_DOWNLOAD_URL = Tables.TRACKS + "_" + DOWNLOAD_URL;
          public static final String ALIAS_STREAM_URL = Tables.TRACKS + "_" + STREAM_URL;
          public static final String ALIAS_STREAMABLE = Tables.TRACKS + "_" + STREAMABLE;
          public static final String ALIAS_SHARING = Tables.TRACKS + "_" + SHARING;
          public static final String ALIAS_USER_ID = Tables.TRACKS + "_" + USER_ID;
          public static final String ALIAS_USER_FAVORITE = Tables.TRACKS + "_" + USER_FAVORITE;
          public static final String ALIAS_USER_PLAYED = Tables.TRACKS + "_" + USER_PLAYED;
          public static final String ALIAS_FILELENGTH = Tables.TRACKS + "_" + FILELENGTH;
      }

      public static final class TrackPlays implements BaseColumns {

          public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.track_plays";
          public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.track_plays";

          public static final String ID = "_id";
          public static final String TRACK_ID = "track_id";
          public static final String USER_ID = "user_id";

          public static final String CONCRETE_ID = Tables.TRACK_PLAYS + "." + _ID;
          public static final String CONCRETE_TRACK_ID = Tables.TRACK_PLAYS + "." + TRACK_ID;
          public static final String CONCRETE_USER_ID = Tables.TRACK_PLAYS + "." + USER_ID;

          public static final String ALIAS_ID = Tables.TRACK_PLAYS + "_" + _ID;
          public static final String ALIAS_TRACK_ID = Tables.TRACK_PLAYS + "_" + TRACK_ID;
          public static final String ALIAS_USER_ID = Tables.TRACK_PLAYS + "_" + USER_ID;
      }

      public static final class Users implements BaseColumns {

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
          public static final String CONCRETE_USERNAME = Tables.USERS + "." + USERNAME;
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

          public static final String ALIAS_ID = Tables.USERS + "_" + _ID;
          public static final String ALIAS_USERNAME = Tables.USERS + "_" + USERNAME;
          public static final String ALIAS_PERMALINK = Tables.USERS + "_" + PERMALINK;
          public static final String ALIAS_AVATAR_URL = Tables.USERS + "_" + AVATAR_URL;
          public static final String ALIAS_CITY = Tables.USERS + "_" + CITY;
          public static final String ALIAS_COUNTRY = Tables.USERS + "_" + COUNTRY;
          public static final String ALIAS_DISCOGS_NAME = Tables.USERS + "_" + DISCOGS_NAME;
          public static final String ALIAS_FOLLOWERS_COUNT = Tables.USERS + "_" + FOLLOWERS_COUNT;
          public static final String ALIAS_FOLLOWINGS_COUNT = Tables.USERS + "_" + FOLLOWINGS_COUNT;
          public static final String ALIAS_FULL_NAME = Tables.USERS + "_" + FULL_NAME;
          public static final String ALIAS_MYSPACE_NAME = Tables.USERS + "_" + MYSPACE_NAME;
          public static final String ALIAS_TRACK_COUNT = Tables.USERS + "_" + TRACK_COUNT;
          public static final String ALIAS_WEBSITE = Tables.USERS + "_" + WEBSITE;
          public static final String ALIAS_WEBSITE_TITLE = Tables.USERS + "_" + WEBSITE_TITLE;
          public static final String ALIAS_DESCRIPTION = Tables.USERS + "_" + DESCRIPTION;
      }

      public static final class Recordings implements BaseColumns {

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

          public static final String ALIAS_ID = Tables.RECORDINGS + "_" + ID;
          public static final String ALIAS_USER_ID = Tables.RECORDINGS + "_" + USER_ID;
          public static final String ALIAS_TIMESTAMP = Tables.RECORDINGS + "_" + TIMESTAMP;
          public static final String ALIAS_LONGITUDE = Tables.RECORDINGS + "_" + LONGITUDE;
          public static final String ALIAS_LATITUDE = Tables.RECORDINGS + "_" + LATITUDE;
          public static final String ALIAS_WHAT_TEXT = Tables.RECORDINGS + "_" + WHAT_TEXT;
          public static final String ALIAS_WHERE_TEXT = Tables.RECORDINGS + "_" + WHERE_TEXT;
          public static final String ALIAS_AUDIO_PATH = Tables.RECORDINGS + "_" + AUDIO_PATH;
          public static final String ALIAS_DURATION = Tables.RECORDINGS + "_" + DURATION;
          public static final String ALIAS_ARTWORK_PATH = Tables.RECORDINGS + "_" + ARTWORK_PATH;
          public static final String ALIAS_FOUR_SQUARE_VENUE_ID = Tables.RECORDINGS + "_" + FOUR_SQUARE_VENUE_ID;
          public static final String ALIAS_SHARED_EMAILS = Tables.RECORDINGS + "_" + SHARED_EMAILS;
          public static final String ALIAS_SERVICE_IDS = Tables.RECORDINGS + "_" + SERVICE_IDS;
          public static final String ALIAS_IS_PRIVATE = Tables.RECORDINGS + "_" + IS_PRIVATE;
          public static final String ALIAS_EXTERNAL_UPLOAD = Tables.RECORDINGS + "_" + EXTERNAL_UPLOAD;
          public static final String ALIAS_AUDIO_PROFILE = Tables.RECORDINGS + "_" + AUDIO_PROFILE;
          public static final String ALIAS_UPLOAD_STATUS = Tables.RECORDINGS + "_" + UPLOAD_STATUS;
          public static final String ALIAS_UPLOAD_ERROR = Tables.RECORDINGS + "_" + UPLOAD_ERROR;
      }

      public static final class Events implements BaseColumns {
          public static final Uri CONTENT_URI = Uri.parse("content://"
                  + ScContentProvider.AUTHORITY + "/Events");

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

          public static final String ALIAS_ID = Tables.EVENTS + "_" + ID;
          public static final String ALIAS_USER_ID = Tables.EVENTS + "_" + USER_ID;
          public static final String ALIAS_TYPE = Tables.EVENTS + "_" + TYPE;
          public static final String ALIAS_CREATED_AT = Tables.EVENTS + "_" + CREATED_AT;
          public static final String ALIAS_EXCLUSIVE = Tables.EVENTS + "_" + EXCLUSIVE;
          public static final String ALIAS_TAGS = Tables.EVENTS + "_" + TAGS;
          public static final String ALIAS_LABEL = Tables.EVENTS + "_" + LABEL;
          public static final String ALIAS_ORIGIN_ID = Tables.EVENTS + "_" + ORIGIN_ID;
          public static final String ALIAS_NEXT_CURSOR = Tables.EVENTS + "_" + NEXT_CURSOR;
      }


}
