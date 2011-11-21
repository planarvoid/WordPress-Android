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

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "ScContentProvider";
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 9;


    public enum Tables {
        TRACKS("Tracks", DATABASE_CREATE_TRACKS),
        RECORDINGS("Recordings", DATABASE_CREATE_RECORDINGS),
        USERS("Users", DATABASE_CREATE_USERS),
        TRACK_PLAYS("TrackPlays", DATABASE_CREATE_TRACK_PLAYS),
        EVENTS("Events", DATABASE_CREATE_EVENTS),
        SEARCHES("Searches", DATABASE_CREATE_SEARCHES),

        USER_FAVORITES("UserFavorites", DATABASE_CREATE_USER_FAVORITES),
        USER_FOLLOWING("UserFollowing", DATABASE_CREATE_USER_FOLLOWING),
        USER_FOLLOWERS("UserFollowers", DATABASE_CREATE_USER_FOLLOWERS),

        TRACKVIEW("TrackView", null);
        //TRACKLISTVIEW("TracklistView", DATABASE_CREATE_TRACKLIST_VIEW),
        //EVENTLISTVIEW("EventlistView", DATABASE_CREATE_EVENTLIST_VIEW);

        public final String tableName;
        public final String createString;


        Tables(String name, String create) {
            tableName = name;
            createString = create;
        }

        public static Tables get(String name) {
            EnumSet<DBHelper.Tables> tables = EnumSet.allOf(DBHelper.Tables.class);
            for (Tables table : tables) {
                if (table.tableName.equals(name)) return table;
            }
            return null;
        }
    }


    DBHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.i("asdf","Making trackview "+ DATABASE_CREATE_TRACK_VIEW);
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
            db.execSQL(DATABASE_CREATE_USERS);
            db.execSQL(DATABASE_CREATE_RECORDINGS);
            db.execSQL(DATABASE_CREATE_SEARCHES);
            db.execSQL(DATABASE_CREATE_USER_FAVORITES);
            db.execSQL(DATABASE_CREATE_TRACK_VIEW);
        } catch (SQLException e) {
            throw new RuntimeException(e);
            //SoundCloudApplication.handleSilentException("error during onCreate()", e);
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
                        case 7:
                            success = upgradeTo7(db, oldVersion);
                            break;
                        case 8:
                            success = upgradeTo8(db, oldVersion);
                            break;
                        case 9:
                            success = upgradeTo9(db, oldVersion);
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
        db.execSQL("DROP TABLE IF EXISTS Searches");
        db.execSQL("DROP TABLE IF EXISTS UserFavorites");
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

    private boolean upgradeTo7(SQLiteDatabase db, int oldVersion) {
            try {
                alterTableColumns(db, Tables.RECORDINGS, null, null);
                return true;
            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade7 " +
                        "(from "+oldVersion+")", e);
            }
            return false;
        }

    private boolean upgradeTo8(SQLiteDatabase db, int oldVersion) {
            try {
                db.execSQL(DATABASE_CREATE_SEARCHES);
                return true;
            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade8 " +
                        "(from "+oldVersion+")", e);
            }
            return false;
        }

    private boolean upgradeTo9(SQLiteDatabase db, int oldVersion) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER_FAVORITES.tableName);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER_FOLLOWERS.tableName);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER_FOLLOWING.tableName);
                db.execSQL("DROP VIEW IF EXISTS " + Tables.TRACKVIEW.tableName);
                db.execSQL(DATABASE_CREATE_USER_FAVORITES);
                db.execSQL(DATABASE_CREATE_USER_FOLLOWERS);
                db.execSQL(DATABASE_CREATE_USER_FOLLOWING);
                db.execSQL(DATABASE_CREATE_TRACK_VIEW);
                return false;

            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade8 " +
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


    static final String DATABASE_CREATE_EVENTS = "create table Events (_id INTEGER primary key AUTOINCREMENT, "
        + "created_at INTEGER null, "
        + "user_id INTEGER null, "
        + "type VARCHAR(255) null, "
        + "exclusive boolean false, "
        + "origin_id INTEGER null, "
        + "tags VARCHAR(255) null, "
        + "label VARCHAR(255) null, "
        + "next_cursor VARCHAR(255) null);";


    static final String DATABASE_CREATE_TRACKS = "create table Tracks (_id INTEGER primary key, "
            + "permalink VARCHAR(255) null, "
            + "duration INTEGER null, "
            + "created_at INTEGER null, "
            + "tag_list VARCHAR(255) null, "
            + "track_type VARCHAR(255) null, "
            + "title VARCHAR(255) null, "
            + "permalink_url VARCHAR(255) null, "
            + "artwork_url VARCHAR(255) null, "
            + "waveform_url VARCHAR(255) null, "
            + "downloadable VARCHAR(255) null, "
            + "download_url VARCHAR(255) null, "
            + "stream_url VARCHAR(255) null, "
            + "streamable VARCHAR(255) null, "
            + "sharing VARCHAR(255) null, "
            + "user_id INTEGER null, "
            + "user_favorite BOOLEAN DEFAULT FALSE, "
            + "filelength INTEGER null);";

    static final String DATABASE_CREATE_TRACK_PLAYS = "create table TrackPlays (_id INTEGER primary key AUTOINCREMENT, "
            + "track_id INTEGER null, " + "user_id INTEGER null);";

    static final String DATABASE_CREATE_USERS = "create table Users (_id INTEGER primary key, "
            + "username VARCHAR(255) null, "
            + "avatar_url VARCHAR(255) null, "
            + "permalink VARCHAR(255) null, "
            + "city VARCHAR(255) null, "
            + "country VARCHAR(255) null, "
            + "discogs_name VARCHAR(255) null, "
            + "followers_count INTEGER null, "
            + "followings_count INTEGER null, "
            + "full_name VARCHAR(255) null, "
            + "myspace_name VARCHAR(255) null, "
            + "track_count INTEGER null, "
            + "website VARCHAR(255) null, "
            + "website_title VARCHAR(255) null, "
            + "description text null);";

    static final String DATABASE_CREATE_RECORDINGS = "create table Recordings (_id INTEGER primary key AUTOINCREMENT, "
            + "user_id INTEGER null, "
            + "timestamp INTEGER null, "
            + "longitude VARCHAR(255) null, "
            + "latitude VARCHAR(255) null, "
            + "what_text VARCHAR(255) null, "
            + "where_text VARCHAR(255) null, "
            + "audio_path VARCHAR(255) null, "
            + "artwork_path VARCHAR(255) null, "
            + "duration INTEGER null, "
            + "four_square_venue_id VARCHAR(255) null, "
            + "shared_emails text null, "
            + "shared_ids text null, "
            + "private_user_id INTEGER null, "
            + "service_ids VARCHAR(255) null, "
            + "is_private boolean default false, "
            + "external_upload boolean default false, "
            + "audio_profile INTEGER null, "
            + "upload_status INTEGER default 0, "
            + "upload_error boolean default false);";

    static final String DATABASE_CREATE_SEARCHES = "create table Searches (_id INTEGER primary key AUTOINCREMENT, "
            + "created_at INTEGER null, "
            + "user_id INTEGER null, "
            + "query VARCHAR(255) null, "
            + "search_type INTEGER null);";

    static final String DATABASE_CREATE_USER_FAVORITES = "create table UserFavorites (_id INTEGER primary key AUTOINCREMENT, "
            + "track_id INTEGER null, " + "user_id INTEGER null);";

    static final String DATABASE_CREATE_USER_FOLLOWING = "create table UserFollowings (_id INTEGER primary key AUTOINCREMENT, "
            + "user_id INTEGER null, " + "following_id INTEGER null);";

    static final String DATABASE_CREATE_USER_FOLLOWERS = "create table UserFollowers (_id INTEGER primary key AUTOINCREMENT, "
            + "user_id INTEGER null, " + "follower_id INTEGER null);";


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
          public static final String FILELENGTH = "filelength";

          public static final String CONCRETE_ID = Tables.TRACKS.tableName + "." + ID;
          public static final String CONCRETE_PERMALINK = Tables.TRACKS.tableName + "." + PERMALINK;
          public static final String CONCRETE_CREATED_AT = Tables.TRACKS.tableName + "." + CREATED_AT;
          public static final String CONCRETE_DURATION = Tables.TRACKS.tableName + "." + DURATION;
          public static final String CONCRETE_TAG_LIST = Tables.TRACKS.tableName + "." + TAG_LIST;
          public static final String CONCRETE_TRACK_TYPE = Tables.TRACKS.tableName + "." + TRACK_TYPE;
          public static final String CONCRETE_TITLE = Tables.TRACKS.tableName + "." + TITLE;
          public static final String CONCRETE_PERMALINK_URL = Tables.TRACKS.tableName + "." + PERMALINK_URL;
          public static final String CONCRETE_ARTWORK_URL = Tables.TRACKS.tableName + "." + ARTWORK_URL;
          public static final String CONCRETE_WAVEFORM_URL = Tables.TRACKS.tableName + "." + WAVEFORM_URL;
          public static final String CONCRETE_DOWNLOADABLE = Tables.TRACKS.tableName + "." + DOWNLOADABLE;
          public static final String CONCRETE_DOWNLOAD_URL = Tables.TRACKS.tableName + "." + DOWNLOAD_URL;
          public static final String CONCRETE_STREAM_URL = Tables.TRACKS.tableName + "." + STREAM_URL;
          public static final String CONCRETE_STREAMABLE = Tables.TRACKS.tableName + "." + STREAMABLE;
          public static final String CONCRETE_SHARING = Tables.TRACKS.tableName + "." + SHARING;
          public static final String CONCRETE_USER_ID = Tables.TRACKS.tableName + "." + USER_ID;
          public static final String CONCRETE_FILELENGTH = Tables.TRACKS.tableName + "." + FILELENGTH;

          public static final String ALIAS_ID = Tables.TRACKS.tableName + "_" + ID;
          public static final String ALIAS_PERMALINK = Tables.TRACKS.tableName + "_" + PERMALINK;
          public static final String ALIAS_CREATED_AT = Tables.TRACKS.tableName + "_" + CREATED_AT;
          public static final String ALIAS_DURATION = Tables.TRACKS.tableName + "_" + DURATION;
          public static final String ALIAS_TAG_LIST = Tables.TRACKS.tableName + "_" + TAG_LIST;
          public static final String ALIAS_TRACK_TYPE = Tables.TRACKS.tableName + "_" + TRACK_TYPE;
          public static final String ALIAS_TITLE = Tables.TRACKS.tableName + "_" + TITLE;
          public static final String ALIAS_PERMALINK_URL = Tables.TRACKS.tableName + "_" + PERMALINK_URL;
          public static final String ALIAS_ARTWORK_URL = Tables.TRACKS.tableName + "_" + ARTWORK_URL;
          public static final String ALIAS_WAVEFORM_URL = Tables.TRACKS.tableName + "_" + WAVEFORM_URL;
          public static final String ALIAS_DOWNLOADABLE = Tables.TRACKS.tableName + "_" + DOWNLOADABLE;
          public static final String ALIAS_DOWNLOAD_URL = Tables.TRACKS.tableName + "_" + DOWNLOAD_URL;
          public static final String ALIAS_STREAM_URL = Tables.TRACKS.tableName + "_" + STREAM_URL;
          public static final String ALIAS_STREAMABLE = Tables.TRACKS.tableName + "_" + STREAMABLE;
          public static final String ALIAS_SHARING = Tables.TRACKS.tableName + "_" + SHARING;
          public static final String ALIAS_USER_ID = Tables.TRACKS.tableName + "_" + USER_ID;
          public static final String ALIAS_FILELENGTH = Tables.TRACKS.tableName + "_" + FILELENGTH;
      }

      public static final class TrackPlays implements BaseColumns {

          public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.track_plays";
          public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.track_plays";

          public static final String ID = "_id";
          public static final String TRACK_ID = "track_id";
          public static final String USER_ID = "user_id";

          public static final String CONCRETE_ID = Tables.TRACK_PLAYS.tableName + "." + _ID;
          public static final String CONCRETE_TRACK_ID = Tables.TRACK_PLAYS.tableName + "." + TRACK_ID;
          public static final String CONCRETE_USER_ID = Tables.TRACK_PLAYS.tableName + "." + USER_ID;

          public static final String ALIAS_ID = Tables.TRACK_PLAYS.tableName + "_" + _ID;
          public static final String ALIAS_TRACK_ID = Tables.TRACK_PLAYS.tableName + "_" + TRACK_ID;
          public static final String ALIAS_USER_ID = Tables.TRACK_PLAYS.tableName + "_" + USER_ID;
      }

    public static final class UserFavorites implements BaseColumns {

          public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.user_favorites";
          public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.user_favorites";

          public static final String ID = "_id";
          public static final String TRACK_ID = "track_id";
          public static final String USER_ID = "user_id";

          public static final String CONCRETE_ID = Tables.USER_FAVORITES.tableName + "." + _ID;
          public static final String CONCRETE_TRACK_ID = Tables.USER_FAVORITES.tableName + "." + TRACK_ID;
          public static final String CONCRETE_USER_ID = Tables.USER_FAVORITES.tableName + "." + USER_ID;

          public static final String ALIAS_ID = Tables.USER_FAVORITES.tableName + "_" + _ID;
          public static final String ALIAS_TRACK_ID = Tables.USER_FAVORITES.tableName + "_" + TRACK_ID;
          public static final String ALIAS_USER_ID = Tables.USER_FAVORITES.tableName + "_" + USER_ID;
      }

    public static final class UserFollowing implements BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.user_favorites";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.user_favorites";

        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String FOLLOWING_ID = "following_id";

        public static final String CONCRETE_ID = Tables.USER_FOLLOWING.tableName + "." + _ID;
        public static final String CONCRETE_USER_ID = Tables.USER_FOLLOWING.tableName + "." + USER_ID;
        public static final String CONCRETE_FOLLOWING_ID = Tables.USER_FOLLOWING.tableName + "." + FOLLOWING_ID;

        public static final String ALIAS_ID = Tables.USER_FOLLOWING.tableName + "_" + _ID;
        public static final String ALIAS_USER_ID = Tables.USER_FOLLOWING.tableName + "_" + USER_ID;
        public static final String ALIAS_FOLLOWING_ID = Tables.USER_FOLLOWING.tableName + "_" + FOLLOWING_ID;
    }
    
    public static final class UserFollowers implements BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.user_favorites";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.user_favorites";

        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String FOLLOWER_ID = "following_id";

        public static final String CONCRETE_ID = Tables.USER_FOLLOWERS.tableName + "." + _ID;
        public static final String CONCRETE_USER_ID = Tables.USER_FOLLOWERS.tableName + "." + USER_ID;
        public static final String CONCRETE_FOLLOWER_ID = Tables.USER_FOLLOWERS.tableName + "." + FOLLOWER_ID;

        public static final String ALIAS_ID = Tables.USER_FOLLOWERS.tableName + "_" + _ID;
        public static final String ALIAS_USER_ID = Tables.USER_FOLLOWERS.tableName + "_" + USER_ID;
        public static final String ALIAS_FOLLOWER_ID = Tables.USER_FOLLOWERS.tableName + "_" + FOLLOWER_ID;
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

          public static final String CONCRETE_ID = Tables.USERS.tableName + "." + _ID;
          public static final String CONCRETE_USERNAME = Tables.USERS.tableName + "." + USERNAME;
          public static final String CONCRETE_PERMALINK = Tables.USERS.tableName + "." + PERMALINK;
          public static final String CONCRETE_AVATAR_URL = Tables.USERS.tableName + "." + AVATAR_URL;
          public static final String CONCRETE_CITY = Tables.USERS.tableName + "." + CITY;
          public static final String CONCRETE_COUNTRY = Tables.USERS.tableName + "." + COUNTRY;
          public static final String CONCRETE_DISCOGS_NAME = Tables.USERS.tableName + "." + DISCOGS_NAME;
          public static final String CONCRETE_FOLLOWERS_COUNT = Tables.USERS.tableName + "." + FOLLOWERS_COUNT;
          public static final String CONCRETE_FOLLOWINGS_COUNT = Tables.USERS.tableName + "." + FOLLOWINGS_COUNT;
          public static final String CONCRETE_FULL_NAME = Tables.USERS.tableName + "." + FULL_NAME;
          public static final String CONCRETE_MYSPACE_NAME = Tables.USERS.tableName + "." + MYSPACE_NAME;
          public static final String CONCRETE_TRACK_COUNT = Tables.USERS.tableName + "." + TRACK_COUNT;
          public static final String CONCRETE_WEBSITE = Tables.USERS.tableName + "." + WEBSITE;
          public static final String CONCRETE_WEBSITE_TITLE = Tables.USERS.tableName + "." + WEBSITE_TITLE;
          public static final String CONCRETE_DESCRIPTION = Tables.USERS.tableName + "." + DESCRIPTION;

          public static final String ALIAS_ID = Tables.USERS.tableName + "_" + _ID;
          public static final String ALIAS_USERNAME = Tables.USERS.tableName + "_" + USERNAME;
          public static final String ALIAS_PERMALINK = Tables.USERS.tableName + "_" + PERMALINK;
          public static final String ALIAS_AVATAR_URL = Tables.USERS.tableName + "_" + AVATAR_URL;
          public static final String ALIAS_CITY = Tables.USERS.tableName + "_" + CITY;
          public static final String ALIAS_COUNTRY = Tables.USERS.tableName + "_" + COUNTRY;
          public static final String ALIAS_DISCOGS_NAME = Tables.USERS.tableName + "_" + DISCOGS_NAME;
          public static final String ALIAS_FOLLOWERS_COUNT = Tables.USERS.tableName + "_" + FOLLOWERS_COUNT;
          public static final String ALIAS_FOLLOWINGS_COUNT = Tables.USERS.tableName + "_" + FOLLOWINGS_COUNT;
          public static final String ALIAS_FULL_NAME = Tables.USERS.tableName + "_" + FULL_NAME;
          public static final String ALIAS_MYSPACE_NAME = Tables.USERS.tableName + "_" + MYSPACE_NAME;
          public static final String ALIAS_TRACK_COUNT = Tables.USERS.tableName + "_" + TRACK_COUNT;
          public static final String ALIAS_WEBSITE = Tables.USERS.tableName + "_" + WEBSITE;
          public static final String ALIAS_WEBSITE_TITLE = Tables.USERS.tableName + "_" + WEBSITE_TITLE;
          public static final String ALIAS_DESCRIPTION = Tables.USERS.tableName + "_" + DESCRIPTION;
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
          public static final String SHARED_IDS = "shared_ids";
          public static final String PRIVATE_USER_ID = "private_user_id";
          public static final String SERVICE_IDS = "service_ids";
          public static final String IS_PRIVATE = "is_private";
          public static final String EXTERNAL_UPLOAD = "external_upload";
          public static final String AUDIO_PROFILE = "audio_profile";
          public static final String UPLOAD_STATUS = "upload_status";
          public static final String UPLOAD_ERROR = "upload_error";

          public static final String CONCRETE_ID = Tables.RECORDINGS.tableName + "." + ID;
          public static final String CONCRETE_USER_ID = Tables.RECORDINGS.tableName + "." + USER_ID;
          public static final String CONCRETE_TIMESTAMP = Tables.RECORDINGS.tableName + "." + TIMESTAMP;
          public static final String CONCRETE_LONGITUDE = Tables.RECORDINGS.tableName + "." + LONGITUDE;
          public static final String CONCRETE_LATITUDE = Tables.RECORDINGS.tableName + "." + LATITUDE;
          public static final String CONCRETE_WHAT_TEXT = Tables.RECORDINGS.tableName + "." + WHAT_TEXT;
          public static final String CONCRETE_WHERE_TEXT = Tables.RECORDINGS.tableName + "." + WHERE_TEXT;
          public static final String CONCRETE_AUDIO_PATH = Tables.RECORDINGS.tableName + "." + AUDIO_PATH;
          public static final String CONCRETE_DURATION = Tables.RECORDINGS.tableName + "." + DURATION;
          public static final String CONCRETE_ARTWORK_PATH = Tables.RECORDINGS.tableName + "." + ARTWORK_PATH;
          public static final String CONCRETE_FOUR_SQUARE_VENUE_ID = Tables.RECORDINGS.tableName + "." + FOUR_SQUARE_VENUE_ID;
          public static final String CONCRETE_SHARED_EMAILS = Tables.RECORDINGS.tableName + "." + SHARED_EMAILS;
          public static final String CONCRETE_SHARED_IDS = Tables.RECORDINGS.tableName + "." + SHARED_IDS;
          public static final String CONCRETE_PRIVATE_USER_ID = Tables.RECORDINGS.tableName + "." + PRIVATE_USER_ID;
          public static final String CONCRETE_SERVICE_IDS = Tables.RECORDINGS.tableName + "." + SERVICE_IDS;
          public static final String CONCRETE_IS_PRIVATE = Tables.RECORDINGS.tableName + "." + IS_PRIVATE;
          public static final String CONCRETE_EXTERNAL_UPLOAD = Tables.RECORDINGS.tableName + "." + EXTERNAL_UPLOAD;
          public static final String CONCRETE_AUDIO_PROFILE = Tables.RECORDINGS.tableName + "." + AUDIO_PROFILE;
          public static final String CONCRETE_UPLOAD_STATUS = Tables.RECORDINGS.tableName + "." + UPLOAD_STATUS;
          public static final String CONCRETE_UPLOAD_ERROR = Tables.RECORDINGS.tableName + "." + UPLOAD_ERROR;

          public static final String ALIAS_ID = Tables.RECORDINGS.tableName + "_" + ID;
          public static final String ALIAS_USER_ID = Tables.RECORDINGS.tableName + "_" + USER_ID;
          public static final String ALIAS_TIMESTAMP = Tables.RECORDINGS.tableName + "_" + TIMESTAMP;
          public static final String ALIAS_LONGITUDE = Tables.RECORDINGS.tableName + "_" + LONGITUDE;
          public static final String ALIAS_LATITUDE = Tables.RECORDINGS.tableName + "_" + LATITUDE;
          public static final String ALIAS_WHAT_TEXT = Tables.RECORDINGS.tableName + "_" + WHAT_TEXT;
          public static final String ALIAS_WHERE_TEXT = Tables.RECORDINGS.tableName + "_" + WHERE_TEXT;
          public static final String ALIAS_AUDIO_PATH = Tables.RECORDINGS.tableName + "_" + AUDIO_PATH;
          public static final String ALIAS_DURATION = Tables.RECORDINGS.tableName + "_" + DURATION;
          public static final String ALIAS_ARTWORK_PATH = Tables.RECORDINGS.tableName + "_" + ARTWORK_PATH;
          public static final String ALIAS_FOUR_SQUARE_VENUE_ID = Tables.RECORDINGS.tableName + "_" + FOUR_SQUARE_VENUE_ID;
          public static final String ALIAS_SHARED_EMAILS = Tables.RECORDINGS.tableName + "_" + SHARED_EMAILS;
          public static final String ALIAS_SHARED_IDS = Tables.RECORDINGS.tableName + "_" + SHARED_IDS;
          public static final String ALIAS_PRIVATE_USER_ID = Tables.RECORDINGS.tableName + "_" + PRIVATE_USER_ID;
          public static final String ALIAS_SERVICE_IDS = Tables.RECORDINGS.tableName + "_" + SERVICE_IDS;
          public static final String ALIAS_IS_PRIVATE = Tables.RECORDINGS.tableName + "_" + IS_PRIVATE;
          public static final String ALIAS_EXTERNAL_UPLOAD = Tables.RECORDINGS.tableName + "_" + EXTERNAL_UPLOAD;
          public static final String ALIAS_AUDIO_PROFILE = Tables.RECORDINGS.tableName + "_" + AUDIO_PROFILE;
          public static final String ALIAS_UPLOAD_STATUS = Tables.RECORDINGS.tableName + "_" + UPLOAD_STATUS;
          public static final String ALIAS_UPLOAD_ERROR = Tables.RECORDINGS.tableName + "_" + UPLOAD_ERROR;

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

          public static final String CONCRETE_ID = Tables.EVENTS.tableName + "." + ID;
          public static final String CONCRETE_USER_ID = Tables.EVENTS.tableName + "." + USER_ID;
          public static final String CONCRETE_TYPE = Tables.EVENTS.tableName + "." + TYPE;
          public static final String CONCRETE_CREATED_AT = Tables.EVENTS.tableName + "." + CREATED_AT;
          public static final String CONCRETE_EXCLUSIVE = Tables.EVENTS.tableName + "." + EXCLUSIVE;
          public static final String CONCRETE_TAGS = Tables.EVENTS.tableName + "." + TAGS;
          public static final String CONCRETE_LABEL = Tables.EVENTS.tableName + "." + LABEL;
          public static final String CONCRETE_ORIGIN_ID = Tables.EVENTS.tableName + "." + ORIGIN_ID;
          public static final String CONCRETE_NEXT_CURSOR = Tables.EVENTS.tableName + "." + NEXT_CURSOR;

          public static final String ALIAS_ID = Tables.EVENTS.tableName + "_" + ID;
          public static final String ALIAS_USER_ID = Tables.EVENTS.tableName + "_" + USER_ID;
          public static final String ALIAS_TYPE = Tables.EVENTS.tableName + "_" + TYPE;
          public static final String ALIAS_CREATED_AT = Tables.EVENTS.tableName + "_" + CREATED_AT;
          public static final String ALIAS_EXCLUSIVE = Tables.EVENTS.tableName + "_" + EXCLUSIVE;
          public static final String ALIAS_TAGS = Tables.EVENTS.tableName + "_" + TAGS;
          public static final String ALIAS_LABEL = Tables.EVENTS.tableName + "_" + LABEL;
          public static final String ALIAS_ORIGIN_ID = Tables.EVENTS.tableName + "_" + ORIGIN_ID;
          public static final String ALIAS_NEXT_CURSOR = Tables.EVENTS.tableName + "_" + NEXT_CURSOR;
      }

    public static final class Searches implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Searches");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.searches";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.searches";

        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String SEARCH_TYPE = "search_type";
        public static final String CREATED_AT = "created_at";
        public static final String QUERY = "query";

        public static final String CONCRETE_ID = Tables.SEARCHES.tableName + "." + ID;
        public static final String CONCRETE_USER_ID = Tables.SEARCHES.tableName + "." + USER_ID;
        public static final String CONCRETE_TYPE = Tables.SEARCHES.tableName + "." + SEARCH_TYPE;
        public static final String CONCRETE_CREATED_AT = Tables.SEARCHES.tableName + "." + CREATED_AT;
        public static final String CONCRETE_QUERY = Tables.SEARCHES.tableName + "." + QUERY;

        public static final String ALIAS_ID = Tables.SEARCHES.tableName + "_" + ID;
        public static final String ALIAS_USER_ID = Tables.SEARCHES.tableName + "_" + USER_ID;
        public static final String ALIAS_TYPE = Tables.SEARCHES.tableName + "_" + SEARCH_TYPE;
        public static final String ALIAS_CREATED_AT = Tables.SEARCHES.tableName + "_" + CREATED_AT;
        public static final String ALIAS_QUERY = Tables.SEARCHES.tableName + "_" + QUERY;
    }

    public static final class TrackView implements BaseColumns {

        public static final String PERMALINK = Tracks.PERMALINK;
        public static final String CREATED_AT = Tracks.CREATED_AT;
        public static final String DURATION = Tracks.DURATION;
        public static final String TAG_LIST = Tracks.TAG_LIST;
        public static final String TRACK_TYPE = Tracks.TRACK_TYPE;
        public static final String TITLE = Tracks.TITLE;
        public static final String PERMALINK_URL = Tracks.PERMALINK_URL;
        public static final String ARTWORK_URL = Tracks.ARTWORK_URL;
        public static final String WAVEFORM_URL = Tracks.WAVEFORM_URL;
        public static final String DOWNLOADABLE = Tracks.DOWNLOADABLE;
        public static final String DOWNLOAD_URL = Tracks.DOWNLOAD_URL;
        public static final String STREAM_URL = Tracks.STREAM_URL;
        public static final String STREAMABLE = Tracks.STREAMABLE;
        public static final String SHARING = Tracks.SHARING;
        public static final String FILELENGTH = Tracks.FILELENGTH;

        public static final String USER_ID = Tracks.USER_ID;
        public static final String USERNAME = "filelength";
        public static final String USER_PERMALINK = "user_permalink";
        public static final String USER_AVATAR_URL = "user_avatar_url";

        public static final String USER_FAVORITE = "user_favorite";
        public static final String USER_PLAYED = "user_played";
      }


    static final String DATABASE_CREATE_TRACK_VIEW = "CREATE VIEW " + Tables.TRACKVIEW.tableName +
            " AS SELECT "
            + Tracks.CONCRETE_ID + " as " + TrackView._ID + ","
            + Tracks.CONCRETE_PERMALINK + " as " + TrackView.PERMALINK + ","
            + Tracks.CONCRETE_CREATED_AT + " as " + TrackView.CREATED_AT + ","
            + Tracks.CONCRETE_DURATION + " as " + TrackView.DURATION + ","
            + Tracks.CONCRETE_TAG_LIST + " as " + TrackView.TAG_LIST + ","
            + Tracks.CONCRETE_TRACK_TYPE + " as " + TrackView.TRACK_TYPE + ","
            + Tracks.CONCRETE_TITLE + " as " + TrackView.TITLE + ","
            + Tracks.CONCRETE_PERMALINK_URL + " as " + TrackView.PERMALINK_URL + ","
            + Tracks.CONCRETE_ARTWORK_URL + " as " + TrackView.ARTWORK_URL + ","
            + Tracks.CONCRETE_WAVEFORM_URL + " as " + TrackView.WAVEFORM_URL + ","
            + Tracks.CONCRETE_DOWNLOADABLE + " as " + TrackView.DOWNLOADABLE + ","
            + Tracks.CONCRETE_DOWNLOAD_URL + " as " + TrackView.DOWNLOAD_URL + ","
            + Tracks.CONCRETE_STREAM_URL + " as " + TrackView.STREAM_URL + ","
            + Tracks.CONCRETE_STREAMABLE + " as " + TrackView.STREAMABLE + ","
            + Tracks.CONCRETE_SHARING + " as " + TrackView.SHARING + ","
            + Tracks.CONCRETE_FILELENGTH + " as " + TrackView.FILELENGTH + ","
            + Users.CONCRETE_ID + " as " + TrackView.USER_ID + ","
            + Users.CONCRETE_USERNAME + " as " + TrackView.USERNAME + ","
            + Users.CONCRETE_PERMALINK + " as " + TrackView.USER_PERMALINK + ","
            + Users.CONCRETE_AVATAR_URL + " as " + TrackView.USER_AVATAR_URL
            + " FROM " + Tables.TRACKS
            + " JOIN " + Tables.USERS + " ON("
            + Tracks.CONCRETE_USER_ID + " = " + Users.CONCRETE_ID + ")";



    /*

    CREATE VIEW TRACKVIEW AS SELECT TRACKS._id as _id, TRACKS.permalink as permalink, USERS._id as user_id, duration, created_at, tag_list, track_type, title, permalink_url, artwork_url, waveform_url, downloadable, download_url, stream_url, streamable, sharing, user_id, user_favorite, filelength FROM TRACKS JOIN USERS ON(TRACKS.user_id = USERS._id) LEFT OUTER JOIN TRACK_PLAYS ON(TRACKS._id = TRACK_PLAYS.track_id)

    static final String DATABASE_CREATE_EVENTLIST_VIEW = "CREATE VIEW " + Tables.EVENTLISTVIEW +
            " AS SELECT "
                + Events.CONCRETE_ID + " as " + Events.ALIAS_ID + ","
                + Events.CONCRETE_USER_ID + " as " + Events.ALIAS_USER_ID + ","
                + Events.CONCRETE_EXCLUSIVE + " as " + Events.ALIAS_EXCLUSIVE + ","
                + Events.CONCRETE_NEXT_CURSOR + " as " + Events.ALIAS_NEXT_CURSOR + ","
                + Tracks.CONCRETE_ID + " as " + Tracks.ALIAS_ID + ","
                + Tracks.CONCRETE_TITLE + " as " + Tracks.ALIAS_TITLE + ","
                + Users.CONCRETE_USERNAME + " as " + Users.ALIAS_USERNAME + ","
                + Users.CONCRETE_ID + " as " + Users.ALIAS_ID + ","
                + Tracks.CONCRETE_STREAMABLE + " as " + Tracks.ALIAS_STREAMABLE + ","
                + Tracks.CONCRETE_STREAM_URL + " as " + Tracks.ALIAS_STREAM_URL + ","
                + Tracks.CONCRETE_ARTWORK_URL + " as " + Tracks.ALIAS_ARTWORK_URL + ","
                + Tracks.CONCRETE_SHARING + " as " + Tracks.ALIAS_SHARING + ","
                + Tracks.CONCRETE_CREATED_AT + " as " + Tracks.ALIAS_CREATED_AT + ","
                + Tracks.CONCRETE_USER_FAVORITE + " as " + Tracks.ALIAS_USER_FAVORITE + ","
                + Tracks.CONCRETE_DURATION + " as " + Tracks.ALIAS_DURATION
                + " FROM " + Tables.EVENTS
                + " JOIN " + Tables.TRACKS + " ON("
                + Events.CONCRETE_ORIGIN_ID + " = " + Tracks.CONCRETE_ID + ")"
                + " JOIN " + Tables.USERS + " ON("
                + Tracks.CONCRETE_USER_ID + " = " + Users.CONCRETE_ID + ")"
                + " LEFT OUTER JOIN " + Tables.TRACK_PLAYS + " ON("
                + Tracks.CONCRETE_ID + " = " + TrackPlays.CONCRETE_TRACK_ID + ")";
                */


}
