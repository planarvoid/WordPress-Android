package com.soundcloud.android.provider;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.List;


public class DBHelper extends SQLiteOpenHelper {
    static final String TAG = "DBHelper";

    public static final int DATABASE_VERSION = 12;
    private static final String DATABASE_NAME = "SoundCloud";

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            for (Table t : Table.values()) {
                t.create(db);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
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
                        case 10:
                            success = upgradeTo10(db, oldVersion);
                            break;
                        case 11:
                            success = upgradeTo11(db, oldVersion);
                            break;
                         case 12:
                            success = upgradeTo12(db, oldVersion);
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
                Log.i(TAG, "successful db upgrade");
            } else {
                Log.w(TAG, "upgrade not successful, recreating db");
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
            t.drop(db);
        }
        onCreate(db);
    }

    static final String DATABASE_CREATE_TRACKS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "last_updated INTEGER," +
            "permalink VARCHAR(255)," +
            "duration INTEGER," +
            "created_at INTEGER," +
            "tag_list VARCHAR(255)," +
            "track_type VARCHAR(255)," +
            "title VARCHAR(255)," +
            "permalink_url VARCHAR(255)," +
            "artwork_url VARCHAR(255), " +
            "waveform_url VARCHAR(255), " +
            "downloadable BOOLEAN, " +
            "commentable BOOLEAN, " +
            "download_url VARCHAR(255), " +
            "stream_url VARCHAR(255)," +
            "streamable BOOLEAN DEFAULT 0, " +
            "sharing VARCHAR(255)," +
            "playback_count INTEGER," +
            "download_count INTEGER," +
            "comment_count INTEGER," +
            "favoritings_count INTEGER," +
            "shared_to_count INTEGER," +
            "sharing_note_text VARCHAR(255),"+
            "user_id INTEGER" +
            ");";


    static final String DATABASE_CREATE_TRACK_METADATA = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER, "+
            "play_count INTEGER DEFAULT 0,"+
            "cached INTEGER DEFAULT 0," +
            "type INTEGER DEFAULT 0," +
            "etag VARCHAR(34)," +
            "url_hash VARCHAR(32)," +
            "size INTEGER," +
            "bitrate INTEGER," +
            "UNIQUE (_id, user_id) ON CONFLICT IGNORE"+
            ");";


    static final String DATABASE_CREATE_USERS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            // mini representation
            "username VARCHAR(255)," +
            "avatar_url VARCHAR(255)," +
            "permalink VARCHAR(255)," +
            "permalink_url VARCHAR(255)," +

            "full_name VARCHAR(255)," +
            "description text,"+
            "city VARCHAR(255)," +
            "country VARCHAR(255)," +

            "plan VARCHAR(16)," +
            "primary_email_confirmed INTEGER," +

            "website VARCHAR(255)," +
            "website_title VARCHAR(255), " +

            "discogs_name VARCHAR(255)," +
            "myspace_name VARCHAR(255)," +

            // counts
            "track_count INTEGER," +
            "followers_count INTEGER," +
            "followings_count INTEGER," +
            "public_favorites_count INTEGER," +
            "private_tracks_count INTEGER," +

            // internal
            "last_updated INTEGER" +
            ");";

    static final String DATABASE_CREATE_RECORDINGS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER," +
            "timestamp INTEGER," +
            "longitude VARCHAR(255)," +
            "latitude VARCHAR(255)," +
            "what_text VARCHAR(255)," +
            "where_text VARCHAR(255)," +
            "audio_path VARCHAR(255)," +
            "artwork_path VARCHAR(255)," +
            "duration INTEGER," +
            "four_square_venue_id VARCHAR(255), " +
            "shared_emails text," +
            "shared_ids text, " +
            "private_user_id INTEGER," +
            "service_ids VARCHAR(255)," +
            "is_private BOOLEAN," +
            "external_upload BOOLEAN," +
            "audio_profile INTEGER," +
            "upload_status INTEGER DEFAULT 0," +
            "upload_error BOOLEAN"+
            ");";

    static final String DATABASE_CREATE_COMMENTS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER," +
            "track_id INTEGER," +
            "timestamp INTEGER," +
            "created_at INTEGER," +
            "body VARCHAR(255)" +
            ");";

    static final String DATABASE_CREATE_ACTIVITIES = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER," +
            "track_id INTEGER," +
            "comment_id INTEGER," +
            "type VARCHAR(255)," +
            "tags VARCHAR(255)," +
            "created_at INTEGER," +
            "content_id INTEGER," +
            "UNIQUE (created_at, type, content_id, track_id, user_id)" +
            ");";

    static final String DATABASE_CREATE_SEARCHES = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "created_at INTEGER," +
            "user_id INTEGER," +
            "query VARCHAR(255)," +
            "search_type INTEGER," +
            "UNIQUE (user_id, search_type, query) ON CONFLICT REPLACE"+
            ");";

    static final String DATABASE_CREATE_PLAYLIST = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "created_at INTEGER," +
            "position INTEGER," +
            "seek_pos INTEGER," +
            "user_id INTEGER"+
            ");";

    static final String DATABASE_CREATE_PLAYLIST_ITEMS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "playlist_id INTEGER," +
            "track_id INTEGER," +
            "position INTEGER," +
            "user_id INTEGER, "+
            "UNIQUE (track_id, position, user_id) ON CONFLICT IGNORE" +
            ");";

    /**
     * {@link DBHelper.Collections}
     */
    static final String DATABASE_CREATE_COLLECTIONS = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uri VARCHAR(255)," +
            "last_addition INTEGER, " +
            "last_sync INTEGER, " +
            "size INTEGER, " +
            "sync_state INTEGER, " +
            "extra VARCHAR(255), " +
            "UNIQUE (uri)"+
            ");";

    /**
     * {@link DBHelper.CollectionPages}
     */
    static final String DATABASE_CREATE_COLLECTION_PAGES = "(" +
            "collection_id INTEGER," +
            "page_index INTEGER," +
            "etag VARCHAR(255), " +
            "size INTEGER, " +
            "PRIMARY KEY(collection_id, page_index) ON CONFLICT REPLACE" +
            ");";

    /**
     * {@link DBHelper.CollectionItems}
     */
    static final String DATABASE_CREATE_COLLECTION_ITEMS = "(" +
            "user_id INTEGER, " +
            "item_id INTEGER," +
            "collection_type INTEGER, " +
            "position INTEGER, " +
            "PRIMARY KEY(user_id, item_id, collection_type) ON CONFLICT REPLACE"+
            ");";


    static final String DATABASE_CREATE_TRACK_VIEW = "AS SELECT " +
            "Tracks." + Tracks._ID + " as " + TrackView._ID +
            ",Tracks." + Tracks.LAST_UPDATED + " as " + TrackView.LAST_UPDATED +
            ",Tracks." + Tracks.PERMALINK + " as " + TrackView.PERMALINK +
            ",Tracks." + Tracks.CREATED_AT + " as " + TrackView.CREATED_AT +
            ",Tracks." + Tracks.DURATION + " as " + TrackView.DURATION +
            ",Tracks." + Tracks.TAG_LIST + " as " + TrackView.TAG_LIST +
            ",Tracks." + Tracks.TRACK_TYPE + " as " + TrackView.TRACK_TYPE +
            ",Tracks." + Tracks.TITLE + " as " + TrackView.TITLE +
            ",Tracks." + Tracks.PERMALINK_URL + " as " + TrackView.PERMALINK_URL +
            ",Tracks." + Tracks.ARTWORK_URL + " as " + TrackView.ARTWORK_URL +
            ",Tracks." + Tracks.WAVEFORM_URL + " as " + TrackView.WAVEFORM_URL +
            ",Tracks." + Tracks.DOWNLOADABLE + " as " + TrackView.DOWNLOADABLE +
            ",Tracks." + Tracks.DOWNLOAD_URL + " as " + TrackView.DOWNLOAD_URL +
            ",Tracks." + Tracks.STREAM_URL + " as " + TrackView.STREAM_URL +
            ",Tracks." + Tracks.STREAMABLE + " as " + TrackView.STREAMABLE +
            ",Tracks." + Tracks.COMMENTABLE + " as " + TrackView.COMMENTABLE +
            ",Tracks." + Tracks.SHARING + " as " + TrackView.SHARING +
            ",Tracks." + Tracks.PLAYBACK_COUNT + " as " + TrackView.PLAYBACK_COUNT +
            ",Tracks." + Tracks.DOWNLOAD_COUNT + " as " + TrackView.DOWNLOAD_COUNT +
            ",Tracks." + Tracks.COMMENT_COUNT + " as " + TrackView.COMMENT_COUNT +
            ",Tracks." + Tracks.FAVORITINGS_COUNT + " as " + TrackView.FAVORITINGS_COUNT +
            ",Tracks." + Tracks.SHARED_TO_COUNT + " as " + TrackView.SHARED_TO_COUNT +
            ",Tracks." + Tracks.SHARING_NOTE_TEXT + " as " + TrackView.SHARING_NOTE_TEXT +
            ",Users." + Users._ID + " as " + TrackView.USER_ID +
            ",Users." + Users.USERNAME + " as " + TrackView.USERNAME +
            ",Users." + Users.PERMALINK + " as " + TrackView.USER_PERMALINK +
            ",Users." + Users.AVATAR_URL + " as " + TrackView.USER_AVATAR_URL +
            ",COALESCE(TrackMetadata." + TrackMetadata.PLAY_COUNT + ", 0) as " + TrackView.USER_PLAY_COUNT +
            ",COALESCE(TrackMetadata." + TrackMetadata.CACHED + ", 0) as " + TrackView.CACHED +
            ",COALESCE(TrackMetadata." + TrackMetadata.TYPE + ", 0) as " + TrackView.TYPE +
            " FROM Tracks" +
            " JOIN Users ON(" +
            "   Tracks." + Tracks.USER_ID + " = " + "Users." + Users._ID + ")" +
            " LEFT OUTER JOIN TrackMetadata ON(" +
            "   TrackMetadata." + TrackMetadata._ID + " = " + "Tracks." + TrackView._ID + ")"
            ;

    /** A view which combines activity data + tracks/users/comments */
    static final String DATABASE_CREATE_ACTIVITY_VIEW = "AS SELECT " +
            "Activities." + Activities._ID + " as " + ActivityView._ID +
            ",Activities." + Activities.TYPE + " as " + ActivityView.TYPE+
            ",Activities." + Activities.TAGS + " as " + ActivityView.TAGS+
            ",Activities." + Activities.CREATED_AT + " as " + ActivityView.CREATED_AT+
            ",Activities." + Activities.COMMENT_ID + " as " + ActivityView.COMMENT_ID+
            ",Activities." + Activities.TRACK_ID + " as " + ActivityView.TRACK_ID+
            ",Activities." + Activities.USER_ID + " as " + ActivityView.USER_ID +
            ",Activities." + Activities.CONTENT_ID + " as " + ActivityView.CONTENT_ID +

            // activity user (who commented, favorited etc. on contained following)
            ",Users." + Users.USERNAME + " as " + ActivityView.USER_USERNAME +
            ",Users." + Users.PERMALINK + " as " + ActivityView.USER_PERMALINK +
            ",Users." + Users.AVATAR_URL + " as " + ActivityView.USER_AVATAR_URL +

            // track+user data
            ",TrackView.*" +

            // comment data (only for type=comment)
            ",Comments." + Comments.BODY + " as " + ActivityView.COMMENT_BODY +
            ",Comments." + Comments.CREATED_AT + " as " + ActivityView.COMMENT_CREATED_AT +
            ",Comments." + Comments.TIMESTAMP + " as " +ActivityView.COMMENT_TIMESTAMP +
            " FROM Activities" +
            " JOIN Users ON(" +
            "   Activities." + Activities.USER_ID + " = " + "Users." + Users._ID + ")" +
            " JOIN TrackView ON(" +
            "   Activities." + Activities.TRACK_ID + " = " + "TrackView." + TrackView._ID + ")" +
            " LEFT JOIN Comments ON(" +
            "   Activities." + Activities.COMMENT_ID + " = " + "Comments." + Comments._ID + ")" +
            " LEFT JOIN Activities dup ON(" +
            "   dup.track_id = Activities.track_id AND dup.type='track-sharing' AND Activities.type = 'track')" +
            " WHERE dup._id IS NULL" +
            " ORDER BY " + ActivityView.CREATED_AT + " DESC"
            ;

    public static class ResourceTable implements BaseColumns {
        public static final String CREATED_AT = "created_at";
        public static final String LAST_UPDATED = "last_updated";
        public static final String PERMALINK    = "permalink";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_TRACKS}
     */
    public static final class Tracks extends ResourceTable  {
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
        public static final String COMMENTABLE = "commentable";
        public static final String SHARING = "sharing";
        public static final String PLAYBACK_COUNT = "playback_count";
        public static final String DOWNLOAD_COUNT = "download_count";
        public static final String COMMENT_COUNT = "comment_count";
        public static final String FAVORITINGS_COUNT = "favoritings_count";
        public static final String SHARED_TO_COUNT = "shared_to_count";
        public static final String SHARING_NOTE_TEXT = "sharing_note_text";
        public static final String USER_ID = "user_id";

        public static final String[] ALL_FIELDS = {
                _ID, DURATION, TAG_LIST, TRACK_TYPE, TITLE, PERMALINK_URL, ARTWORK_URL,
                WAVEFORM_URL, DOWNLOADABLE, DOWNLOAD_URL, STREAM_URL, STREAM_URL,
                STREAMABLE, COMMENTABLE, SHARING, PLAYBACK_COUNT, DOWNLOAD_COUNT,
                COMMENT_COUNT, FAVORITINGS_COUNT, SHARED_TO_COUNT, SHARING_NOTE_TEXT,
                USER_ID, CREATED_AT, PERMALINK, LAST_UPDATED
        };
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_TRACK_METADATA}
     */
    public static final class TrackMetadata implements BaseColumns {
        public static final String USER_ID    = "user_id";
        public static final String PLAY_COUNT = "play_count";
        public static final String CACHED     = "cached";
        public static final String TYPE       = "type";
        public static final String SIZE       = "size";
        public static final String URL_HASH   = "url_hash";
        public static final String ETAG       = "etag";
        public static final String BITRATE    = "bitrate";

        public static final String[] ALL_FIELDS = new String[] {
            _ID, USER_ID, PLAY_COUNT, CACHED, TYPE, ETAG, BITRATE, URL_HASH, SIZE
        };
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_COLLECTION_ITEMS}
     */
    public static final class CollectionItems {
        public static final String ITEM_ID = "item_id";
        public static final String USER_ID = "user_id";     // "owner" of the item
        public static final String COLLECTION_TYPE = "collection_type";
        public static final String POSITION = "position";

        public static final String SORT_ORDER = POSITION + " ASC";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_USERS}
     */
    public static final class Users extends ResourceTable  {
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
        public static final String PERMALINK_URL = "permalink_url";

        public static final String PRIMARY_EMAIL_CONFIRMED = "primary_email_confirmed";
        public static final String PUBLIC_FAVORITES_COUNT = "public_favorites_count";
        public static final String PRIVATE_TRACKS_COUNT = "private_tracks_count";

        public static final String PLAN = "plan";

        public static final String[] ALL_FIELDS = {
                _ID, USERNAME, AVATAR_URL, CITY, COUNTRY, DISCOGS_NAME,
                FOLLOWERS_COUNT, FOLLOWINGS_COUNT, FULL_NAME, MYSPACE_NAME,
                TRACK_COUNT, WEBSITE, WEBSITE_TITLE, DESCRIPTION, PERMALINK,
                LAST_UPDATED, PERMALINK_URL, PRIMARY_EMAIL_CONFIRMED, PUBLIC_FAVORITES_COUNT,
                PRIVATE_TRACKS_COUNT, PLAN
        };
    }

    public static final class Comments extends ResourceTable {
        public static final String BODY = "body";
        public static final String TIMESTAMP = "timestamp";
        public static final String USER_ID = "user_id";
        public static final String TRACK_ID = "track_id";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_RECORDINGS}
     */
    public static final class Recordings implements BaseColumns {
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

    /**
     * {@link DBHelper.DATABASE_CREATE_ACTIVITIES}
     */
    public static class Activities implements BaseColumns {
        public static final String TYPE = "type";
        public static final String TAGS = "tags";
        public static final String USER_ID = "user_id";
        public static final String TRACK_ID = "track_id";
        public static final String COMMENT_ID = "comment_id";
        public static final String CREATED_AT  = "created_at";
        public static final String CONTENT_ID  = "content_id";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_SEARCHES}
     */
    public static final class Searches implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String SEARCH_TYPE = "search_type";
        public static final String CREATED_AT = "created_at";
        public static final String QUERY = "query";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_COLLECTIONS}
     */
    public static final class Collections implements BaseColumns {
        public static final String URI = "uri";                      // local content provider uri
        public static final String LAST_ADDITION = "last_addition";  // last addition (from API, not used)
        public static final String LAST_SYNC = "last_sync";          // timestamp of last sync
        public static final String SIZE = "size";
        public static final String SYNC_STATE = "sync_state";        // are we currently syncing?
        public static final String EXTRA = "extra";                  // general purpose field
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_COLLECTION_PAGES}
     */
    public static final class CollectionPages implements BaseColumns {
        public static final String COLLECTION_ID = "collection_id";
        public static final String ETAG = "etag";
        public static final String SIZE = "size";
        public static final String PAGE_INDEX = "page_index";
    }

    /**
     * {@link DBHelper.DATABASE_CREATE_PLAYLIST_ITEMS}
     */
    public final static class PlaylistItems implements BaseColumns{
        public static final String PLAYLIST_ID = "playlist_id";
        public static final String TRACK_ID = "track_id";
        public static final String POSITION = "position";
        public static final String USER_ID = "user_id";
    }

    public final static class TrackView implements BaseColumns {
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
        public static final String COMMENTABLE = Tracks.COMMENTABLE;
        public static final String SHARING = Tracks.SHARING;
        public static final String PLAYBACK_COUNT = Tracks.PLAYBACK_COUNT;
        public static final String DOWNLOAD_COUNT = Tracks.DOWNLOAD_COUNT;
        public static final String COMMENT_COUNT = Tracks.COMMENT_COUNT;
        public static final String FAVORITINGS_COUNT = Tracks.FAVORITINGS_COUNT;
        public static final String SHARED_TO_COUNT = Tracks.SHARED_TO_COUNT;
        public static final String SHARING_NOTE_TEXT = Tracks.SHARING_NOTE_TEXT;

        public static final String USER_ID         = "track_user_id";
        public static final String USERNAME        = "track_user_username";
        public static final String USER_PERMALINK  = "track_user_permalink";

        public static final String USER_AVATAR_URL = "track_user_avatar_url";
        public static final String USER_FAVORITE   = "track_user_favorite";
        public static final String USER_PLAY_COUNT = "track_user_play_count";

        public static final String CACHED          = "track_cached";
        public static final String TYPE            = "track_type";
    }

    public final static class ActivityView extends Activities {
        public static final String COMMENT_BODY = "comment_body";
        public static final String COMMENT_TIMESTAMP = "comment_timestamp";
        public static final String COMMENT_CREATED_AT = "comment_created_at";

        public static final String USER_USERNAME = "activity_user_username";
        public static final String USER_PERMALINK = "activity_user_permalink";
        public static final String USER_AVATAR_URL = "activity_user_avatar_url";
    }


    /*
    * altered id naming for content resolver
    */
    private static boolean upgradeTo4(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TRACKS.alterColumns(db, new String[] { "id" }, new String[] { "_id" });
            Table.USERS.alterColumns(db, new String[] { "id" }, new String[] { "_id" });
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade4 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /*
     * added sharing to database
     */
    private static boolean upgradeTo5(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TRACKS.alterColumns(db, null, null);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade5 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /*
     * added sharing to database
     */
    private static boolean upgradeTo6(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.create(db);
            Table.TRACKS.alterColumns(db);
            Table.USERS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade6 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }


    private static boolean upgradeTo7(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade7 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo8(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SEARCHES.create(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade8 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo9(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TRACK_PLAYS.recreate(db);

            Table.TRACKS.alterColumns(db);
            Table.USERS.alterColumns(db);

            Table.TRACK_VIEW.create(db);
            Table.COMMENTS.create(db);
            Table.ACTIVITIES.create(db);
            Table.ACTIVITY_VIEW.create(db);
            Table.COLLECTIONS.create(db);
            Table.COLLECTION_PAGES.create(db);
            Table.COLLECTION_ITEMS.create(db);
            Table.PLAYLIST.create(db);
            Table.PLAYLIST_ITEMS.create(db);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade9 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
     }

    private static boolean upgradeTo10(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TRACK_PLAYS.drop(db);
            Table.TRACK_METADATA.create(db);
            Table.TRACK_VIEW.recreate(db);
            Table.USERS.alterColumns(db);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade10 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /**
     * Fix for incorrect future-href. Have to clean out activities and stored future-href.
     */
    private static boolean upgradeTo11(SQLiteDatabase db, int oldVersion) {
        try {
            cleanActivities(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade11 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo12(SQLiteDatabase db, int oldVersion) {
        try {
            cleanActivities(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade12 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    public static String getWhereIds(String column, List<Long> idSet){
        StringBuilder sb = new StringBuilder(column + " in (?");
        for (int i = 1; i < idSet.size(); i++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static void cleanActivities(SQLiteDatabase db){
        Table.ACTIVITIES.recreate(db);
        db.execSQL("UPDATE " + Table.COLLECTIONS + " SET " + Collections.EXTRA + " = NULL");
    }
}
