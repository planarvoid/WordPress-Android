package com.soundcloud.android.provider;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "ScContentProvider";
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 9;

    DBHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            for (Table t : Table.values()) {
                Log.d(TAG, "creating "+t);
                db.execSQL(t.createString);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
        for (Table t : Table.values()) {
            db.execSQL("DROP TABLE IF EXISTS "+t.name);
        }
        onCreate(db);
    }

    static final String DATABASE_CREATE_TRACKS = "create table Tracks (_id INTEGER primary key, " +
            "last_updated INTEGER null, " +
            "permalink VARCHAR(255) null, " +
            "duration INTEGER null, " +
            "created_at INTEGER null, " +
            "tag_list VARCHAR(255) null, " +
            "track_type VARCHAR(255) null, " +
            "title VARCHAR(255) null, " +
            "permalink_url VARCHAR(255) null, " +
            "artwork_url VARCHAR(255) null, " +
            "waveform_url VARCHAR(255) null, " +
            "downloadable VARCHAR(255) null, " +
            "download_url VARCHAR(255) null, " +
            "stream_url VARCHAR(255) null, " +
            "streamable VARCHAR(255) null, " +
            "sharing VARCHAR(255) null, " +
            "playback_count INTEGER null, " +
            "download_count INTEGER null, " +
            "comment_count INTEGER null, " +
            "favoritings_count INTEGER null, " +
            "shared_to_count INTEGER null, " +
            "user_id INTEGER null, " +
            "user_favorite BOOLEAN DEFAULT FALSE, " +
            "filelength INTEGER null);";


    static final String DATABASE_CREATE_TRACK_PLAYS = "create table TrackPlays (_id INTEGER primary key AUTOINCREMENT, " +
            "track_id INTEGER null, " +
            "user_id INTEGER null);";

    static final String DATABASE_CREATE_USERS = "create table Users (_id INTEGER primary key, " +
            "last_updated INTEGER null, " +
            "username VARCHAR(255) null, " +
            "avatar_url VARCHAR(255) null, " +
            "permalink VARCHAR(255) null, " +
            "city VARCHAR(255) null, " +
            "country VARCHAR(255) null, " +
            "discogs_name VARCHAR(255) null, " +
            "followers_count INTEGER null, " +
            "followings_count INTEGER null, " +
            "full_name VARCHAR(255) null, " +
            "myspace_name VARCHAR(255) null, " +
            "track_count INTEGER null, " +
            "website VARCHAR(255) null, " +
            "website_title VARCHAR(255) null, " +
            "description text null);";

    static final String DATABASE_CREATE_RECORDINGS = "create table Recordings (_id INTEGER primary key AUTOINCREMENT, " +
            "user_id INTEGER null, " +
            "timestamp INTEGER null, " +
            "longitude VARCHAR(255) null, " +
            "latitude VARCHAR(255) null, " +
            "what_text VARCHAR(255) null, " +
            "where_text VARCHAR(255) null, " +
            "audio_path VARCHAR(255) null, " +
            "artwork_path VARCHAR(255) null, " +
            "duration INTEGER null, " +
            "four_square_venue_id VARCHAR(255) null, " +
            "shared_emails text null, " +
            "shared_ids text null, " +
            "private_user_id INTEGER null, " +
            "service_ids VARCHAR(255) null, " +
            "is_private boolean default false, " +
            "external_upload boolean default false, " +
            "audio_profile INTEGER null, " +
            "upload_status INTEGER default 0, " +
            "upload_error boolean default false);";

    static final String DATABASE_CREATE_COMMENTS = "create table Comments (_id INTEGER primary key AUTOINCREMENT, " +
            "user_id INTEGER null, " +
            "track_id INTEGER null, " +
            "timestamp INTEGER null, " +
            "body VARCHAR(255) null, " +
            "created_at INTEGER null);";


    static final String DATABASE_CREATE_ACTIVITIES = "create table Activities (_id INTEGER primary key AUTOINCREMENT, " +
            "user_id INTEGER null, " +
            "track_id INTEGER null, " +
            "type VARCHAR(255) null, " +
            "tags VARCHAR(255) null, " +
            "created_at INTEGER null);";


    static final String DATABASE_CREATE_SEARCHES = "create table Searches (_id INTEGER primary key AUTOINCREMENT, " +
            "created_at INTEGER null, " +
            "user_id INTEGER null, " +
            "query VARCHAR(255) null, " +
            "search_type INTEGER null);";

    static final String DATABASE_CREATE_PLAYLIST = "create table Playlists (_id INTEGER primary key AUTOINCREMENT, " +
            "created_at INTEGER null, " +
            "position INTEGER null, " +
            "seek_pos INTEGER null, " +
            "user_id INTEGER null);";

    static final String DATABASE_CREATE_PLAYLIST_ITEMS = "create table PlaylistItems (_id INTEGER primary key AUTOINCREMENT, " +
            "playlist_id INTEGER null, " +
            "track_id INTEGER null" +
            "position INTEGER null" +
            "user_id INTEGER null);";

    static final String DATABASE_CREATE_COLLECTIONS = "create table Collections(_id INTEGER primary key AUTOINCREMENT, " +
            "uri VARCHAR(255) null, " +
            "last_addition VARCHAR(255) null, " +
            "size INTEGER null, " +
            "last_sync INTEGER null, " +
            "status INTEGER null, UNIQUE (uri));";

    static final String DATABASE_CREATE_COLLECTION_PAGES = "create table CollectionPages(" +
            "collection_id INTEGER null, " +
            "page_index INTEGER, " +
            "etag VARCHAR(255) null, " +
            "next_href VARCHAR(255) null, " +
            "size INTEGER null, " +
            "PRIMARY KEY(collection_id, page_index) ON CONFLICT REPLACE)";

    static final String DATABASE_CREATE_COLLECTION_ITEMS = "create table CollectionItems(" +
            "user_id INTEGER, " +
            "item_id INTEGER," +
            "collection_type INTEGER, " +
            "position INTEGER null, " +
            "PRIMARY KEY(user_id, item_id, collection_type) ON CONFLICT REPLACE);";


    static final String DATABASE_CREATE_TRACK_VIEW = "CREATE VIEW TrackView" +
            " AS SELECT " +
            "Tracks."+Tracks.ID + " as " + TrackView._ID + "," +
            "Tracks."+Tracks.LAST_UPDATED + " as " + TrackView.LAST_UPDATED + "," +
            "Tracks."+Tracks.PERMALINK + " as " + TrackView.PERMALINK + "," +
            "Tracks."+Tracks.CREATED_AT + " as " + TrackView.CREATED_AT + "," +
            "Tracks."+Tracks.DURATION + " as " + TrackView.DURATION + "," +
            "Tracks."+Tracks.TAG_LIST + " as " + TrackView.TAG_LIST + "," +
            "Tracks."+Tracks.TRACK_TYPE + " as " + TrackView.TRACK_TYPE + "," +
            "Tracks."+Tracks.TITLE + " as " + TrackView.TITLE + "," +
            "Tracks."+Tracks.PERMALINK_URL + " as " + TrackView.PERMALINK_URL + "," +
            "Tracks."+Tracks.ARTWORK_URL + " as " + TrackView.ARTWORK_URL + "," +
            "Tracks."+Tracks.WAVEFORM_URL + " as " + TrackView.WAVEFORM_URL + "," +
            "Tracks."+Tracks.DOWNLOADABLE + " as " + TrackView.DOWNLOADABLE + "," +
            "Tracks."+Tracks.DOWNLOAD_URL + " as " + TrackView.DOWNLOAD_URL + "," +
            "Tracks."+Tracks.STREAM_URL + " as " + TrackView.STREAM_URL + "," +
            "Tracks."+Tracks.STREAMABLE + " as " + TrackView.STREAMABLE + "," +
            "Tracks."+Tracks.SHARING + " as " + TrackView.SHARING + "," +
            "Tracks."+Tracks.PLAYBACK_COUNT + " as " + TrackView.PLAYBACK_COUNT + "," +
            "Tracks."+Tracks.DOWNLOAD_COUNT + " as " + TrackView.DOWNLOAD_COUNT + "," +
            "Tracks."+Tracks.COMMENT_COUNT + " as " + TrackView.COMMENT_COUNT + "," +
            "Tracks."+Tracks.FAVORITINGS_COUNT + " as " + TrackView.FAVORITINGS_COUNT + "," +
            "Tracks."+Tracks.SHARED_TO_COUNT + " as " + TrackView.SHARED_TO_COUNT + "," +
            "Tracks."+Tracks.FILELENGTH + " as " + TrackView.FILELENGTH + "," +
            "Users."+Users.ID + " as " + TrackView.USER_ID + "," +
            "Users."+Users.USERNAME + " as " + TrackView.USERNAME + "," +
            "Users."+Users.PERMALINK + " as " + TrackView.USER_PERMALINK + "," +
            "Users."+Users.AVATAR_URL + " as " + TrackView.USER_AVATAR_URL +
            " FROM Tracks" +
            " JOIN Users ON(" +
            " Tracks."+Tracks.USER_ID + " = " + "Users."+Users.ID + ")";

    public static class ResourceTable {
        public static final String ID = BaseColumns._ID;

        public static final String LAST_UPDATED = "last_updated";
        public static final String PERMALINK = "permalink";
    }

    public static final class Tracks extends ResourceTable implements BaseColumns {
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
        public static final String PLAYBACK_COUNT = "playback_count";
        public static final String DOWNLOAD_COUNT = "download_count";
        public static final String COMMENT_COUNT = "comment_count";
        public static final String FAVORITINGS_COUNT = "favoritings_count";
        public static final String SHARED_TO_COUNT = "shared_to_count";
        public static final String USER_ID = "user_id";
        public static final String FILELENGTH = "filelength";
    }

    public static final class TrackPlays implements BaseColumns {

        public static final String ID = "_id";
        public static final String TRACK_ID = "track_id";
        public static final String USER_ID = "user_id";
    }

    public static final class CollectionItems {
        public static final String ITEM_ID = "item_id";
        public static final String USER_ID = "user_id";
        public static final String COLLECTION_TYPE = "collection_type";
        public static final String POSITION = "position";
    }

      public static final class Users extends ResourceTable implements BaseColumns {

          public static final String USERNAME = "username";
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
          public static final String USER_FOLLOWING = "user_following";
          public static final String USER_FOLLOWER = "user_follower";
      }

      public static final class Recordings implements BaseColumns {

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
      }

    public static final class Searches implements BaseColumns {

        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String SEARCH_TYPE = "search_type";
        public static final String CREATED_AT = "created_at";
        public static final String QUERY = "query";
    }

    public static final class TrackView implements BaseColumns {

        public static final String LAST_UPDATED = Tracks.LAST_UPDATED;
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
        public static final String PLAYBACK_COUNT = Tracks.PLAYBACK_COUNT;
        public static final String DOWNLOAD_COUNT = Tracks.DOWNLOAD_COUNT;
        public static final String COMMENT_COUNT = Tracks.COMMENT_COUNT;
        public static final String FAVORITINGS_COUNT = Tracks.FAVORITINGS_COUNT;
        public static final String SHARED_TO_COUNT = Tracks.SHARED_TO_COUNT;
        public static final String FILELENGTH = Tracks.FILELENGTH;

        public static final String USER_ID = Tracks.USER_ID;
        public static final String USERNAME = Users.USERNAME;

        public static final String USER_PERMALINK = "user_permalink";
        public static final String USER_AVATAR_URL = "user_avatar_url";

        public static final String USER_FAVORITE = "user_favorite";
        public static final String USER_PLAYED = "user_played";
    }

    public static final class Collections implements BaseColumns {
        public static final String ID = "_id";
        public static final String URI = "uri";
        public static final String LAST_ADDITION = "last_addition";
        public static final String LAST_SYNC = "last_sync";
        public static final String SIZE = "size";
        public static final String STATUS = "status";
    }

    public static final class CollectionPages implements BaseColumns {
        public static final String ID = "_id";
        public static final String COLLECTION_ID = "collection_id";
        public static final String ETAG = "etag";
        public static final String NEXT_HREF = "next_href";
        public static final String SIZE = "size";
        public static final String PAGE_INDEX = "page_index";
    }


    /*
    * altered id naming for content resolver
    */
    private static boolean upgradeTo4(SQLiteDatabase db, int oldVersion) {
        try {
            alterTableColumns(db, Table.TRACKS, new String[] {"id" }, new String[] {"_id"});
            alterTableColumns(db, Table.USERS, new String[] {"id"}, new String[] {"_id"});
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
    private static boolean upgradeTo5(SQLiteDatabase db, int oldVersion) {
        try {
            alterTableColumns(db, Table.TRACKS, null, null);
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
    private static boolean upgradeTo6(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(DATABASE_CREATE_RECORDINGS);
            db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
            alterTableColumns(db, Table.TRACKS, null, null);
            alterTableColumns(db, Table.USERS, null, null);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade6 " +
                    "(from "+oldVersion+")", e);
        }
        return false;
    }

    private static boolean upgradeTo7(SQLiteDatabase db, int oldVersion) {
        try {
            alterTableColumns(db, Table.RECORDINGS, null, null);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade7 " +
                    "(from "+oldVersion+")", e);
        }
        return false;
    }

    private static boolean upgradeTo8(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(DATABASE_CREATE_SEARCHES);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade8 " +
                    "(from "+oldVersion+")", e);
        }
        return false;
    }

    private static boolean upgradeTo9(SQLiteDatabase db, int oldVersion) {
        try {
            alterTableColumns(db, Table.TRACKS, null, null);
            alterTableColumns(db, Table.USERS, null, null);
            db.execSQL(DATABASE_CREATE_TRACK_VIEW);
            db.execSQL(DATABASE_CREATE_COLLECTIONS);
            db.execSQL(DATABASE_CREATE_COLLECTION_PAGES);
            db.execSQL(DATABASE_CREATE_COLLECTION_ITEMS);
            db.execSQL(DATABASE_CREATE_PLAYLIST);
            db.execSQL(DATABASE_CREATE_PLAYLIST_ITEMS);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade8 " +
                    "(from "+oldVersion+")", e);
        }
        return false;
    }

    private static String alterTableColumns(SQLiteDatabase db, Table tbl, String[] fromAppendCols,
                                            String[] toAppendCols) {

        db.execSQL("DROP TABLE IF EXISTS bck_" + tbl.name);
        db.execSQL(tbl.createString.replace("create table " + tbl.name, "create table bck_" + tbl.name));
        List<String> columns = getColumnNames(db, "bck_" + tbl.name);
        columns.retainAll(getColumnNames(db, tbl.name));
        String cols = TextUtils.join(",", columns);

        String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", toAppendCols) : cols;

        String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", fromAppendCols) : cols;

        db.execSQL(String.format("INSERT INTO bck_%s (%s) SELECT %s from %s",
                tbl.name, toCols, fromCols, tbl.name));

        db.execSQL("DROP table  '" + tbl.name + "'");
        db.execSQL(tbl.createString);
        db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from bck_%s",
                tbl.name, cols, cols, tbl.name));

        db.execSQL("DROP table bck_" + tbl.name);
        return cols;
    }

    private static List<String> getColumnNames(SQLiteDatabase db, String tableName){
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
}
