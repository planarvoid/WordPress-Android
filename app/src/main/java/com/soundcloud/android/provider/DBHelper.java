package com.soundcloud.android.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.api.Endpoints;

import android.app.SearchManager;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.net.URI;
import java.util.Date;
import java.util.List;


public class DBHelper extends SQLiteOpenHelper {
    static final String TAG = "DBHelper";

    public static final int DATABASE_VERSION = 19;
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
                        case 13:
                            success = upgradeTo13(db, oldVersion);
                            break;
                        case 14:
                            success = upgradeTo14(db, oldVersion);
                            break;
                        case 15:
                            success = upgradeTo15(db, oldVersion);
                            break;
                        case 16:
                            success = upgradeTo16(db, oldVersion);
                            break;
                        case 17:
                            success = upgradeTo17(db, oldVersion);
                            break;
                        case 18:
                            success = upgradeTo18(db, oldVersion);
                            break;
                        case 19:
                            success = upgradeTo19(db, oldVersion);
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
            "_type INTEGER DEFAULT 0," +
            "last_updated INTEGER," +
            "permalink VARCHAR(255)," +
            "duration INTEGER," +
            "state VARCHAR(50)," +
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
            "playback_count INTEGER DEFAULT -1," +
            "download_count INTEGER DEFAULT -1," +
            "comment_count INTEGER DEFAULT -1," +
            "favoritings_count INTEGER DEFAULT -1," +
            "reposts_count INTEGER DEFAULT -1," +
            "shared_to_count INTEGER DEFAULT -1," +
            "sharing_note_text VARCHAR(255),"+
            "tracks_uri VARCHAR(255),"+
            "playlist_type VARCHAR(255),"+
            "user_id INTEGER" +
            ");";

    static final String DATABASE_CREATE_PLAYLIST_TRACKS = "(" +
            "playlist_id INTEGER, " +
            "track_id INTEGER," +
            "position INTEGER," +
            "PRIMARY KEY (track_id, position, playlist_id) ON CONFLICT IGNORE" +
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
            "_type INTEGER," +
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
            "description VARCHAR(255)," +
            "four_square_venue_id VARCHAR(255), " +
            "shared_emails text," +
            "shared_ids text, " +
            "private_user_id INTEGER," +
            "tip_key VARCHAR(255)," +
            "service_ids VARCHAR(255)," +
            "is_private BOOLEAN," +
            "external_upload BOOLEAN," +
            "upload_status INTEGER DEFAULT 0," +
            "trim_left INTEGER," +
            "trim_right INTEGER," +
            "filters INTEGER," +
            "optimize INTEGER," +
            "fading INTEGER" +
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
            "uuid VARCHAR(255)," +
            "user_id INTEGER," +
            "track_id INTEGER," +
            "comment_id INTEGER," +
            "type String," +
            "tags VARCHAR(255)," +
            "created_at INTEGER," +
            "content_id INTEGER," +
            "sharing_note_text VARCHAR(255),"+
            "sharing_note_created_at INTEGER," +
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

    static final String DATABASE_CREATE_PLAY_QUEUE = "("+
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
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
            "last_sync_attempt INTEGER, " +
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
            "resource_type INTEGER DEFAULT 0, " +
            "position INTEGER, " +
            "PRIMARY KEY(user_id, item_id, collection_type) ON CONFLICT REPLACE"+
            ");";

    static final String DATABASE_CREATE_SOUND_VIEW = "AS SELECT " +
            "Sounds." + Sounds._ID + " as " + SoundView._ID +
            ",Sounds." + Sounds._TYPE + " as " + SoundView._TYPE +
            ",Sounds." + Sounds.LAST_UPDATED + " as " + SoundView.LAST_UPDATED +
            ",Sounds." + Sounds.PERMALINK + " as " + SoundView.PERMALINK +
            ",Sounds." + Sounds.CREATED_AT + " as " + SoundView.CREATED_AT +
            ",Sounds." + Sounds.DURATION + " as " + SoundView.DURATION +
            ",Sounds." + Sounds.STATE + " as " + SoundView.STATE +
            ",Sounds." + Sounds.TAG_LIST + " as " + SoundView.TAG_LIST +
            ",Sounds." + Sounds.TRACK_TYPE + " as " + SoundView.TRACK_TYPE +
            ",Sounds." + Sounds.TITLE + " as " + SoundView.TITLE +
            ",Sounds." + Sounds.PERMALINK_URL + " as " + SoundView.PERMALINK_URL +
            ",Sounds." + Sounds.ARTWORK_URL + " as " + SoundView.ARTWORK_URL +
            ",Sounds." + Sounds.WAVEFORM_URL + " as " + SoundView.WAVEFORM_URL +
            ",Sounds." + Sounds.DOWNLOADABLE + " as " + SoundView.DOWNLOADABLE +
            ",Sounds." + Sounds.DOWNLOAD_URL + " as " + SoundView.DOWNLOAD_URL +
            ",Sounds." + Sounds.STREAM_URL + " as " + SoundView.STREAM_URL +
            ",Sounds." + Sounds.STREAMABLE + " as " + SoundView.STREAMABLE +
            ",Sounds." + Sounds.COMMENTABLE + " as " + SoundView.COMMENTABLE +
            ",Sounds." + Sounds.SHARING + " as " + SoundView.SHARING +
            ",Sounds." + Sounds.PLAYBACK_COUNT + " as " + SoundView.PLAYBACK_COUNT +
            ",Sounds." + Sounds.DOWNLOAD_COUNT + " as " + SoundView.DOWNLOAD_COUNT +
            ",Sounds." + Sounds.COMMENT_COUNT + " as " + SoundView.COMMENT_COUNT +
            ",Sounds." + Sounds.LIKES_COUNT + " as " + SoundView.LIKES_COUNT +
            ",Sounds." + Sounds.REPOSTS_COUNT + " as " + SoundView.REPOSTS_COUNT +
            ",Sounds." + Sounds.SHARED_TO_COUNT + " as " + SoundView.SHARED_TO_COUNT +
            ",Users." + Users._ID + " as " + SoundView.USER_ID +
            ",Users." + Users.USERNAME + " as " + SoundView.USERNAME +
            ",Users." + Users.PERMALINK + " as " + SoundView.USER_PERMALINK +
            ",Users." + Users.AVATAR_URL + " as " + SoundView.USER_AVATAR_URL +
            ",COALESCE(TrackMetadata." + TrackMetadata.PLAY_COUNT + ", 0) as " + SoundView.USER_PLAY_COUNT +
            ",COALESCE(TrackMetadata." + TrackMetadata.CACHED + ", 0) as " + SoundView.CACHED +
            ",COALESCE(TrackMetadata." + TrackMetadata.TYPE + ", 0) as " + SoundView._TYPE +
            " FROM Sounds" +
            " LEFT JOIN Users ON(" +
            "   Sounds." + Sounds.USER_ID + " = " + "Users." + Users._ID + ")" +
            " LEFT OUTER JOIN TrackMetadata ON(" +
            "   TrackMetadata." + TrackMetadata._ID + " = " + "Sounds." + SoundView._ID + ")"
            ;

    /** A view which combines activity data + tracks/users/comments */
        static final String DATABASE_CREATE_SOUND_ASSOCIATION_VIEW = "AS SELECT " +
            "CollectionItems." + CollectionItems.POSITION + " as " + SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP +
            ", CollectionItems." + CollectionItems.COLLECTION_TYPE + " as " + SoundAssociationView.SOUND_ASSOCIATION_TYPE +
            ", CollectionItems." + CollectionItems.USER_ID + " as " + SoundAssociationView.SOUND_ASSOCIATION_USER_ID +

            // track+user data
            ", SoundView.*" +
            " FROM " + Table.COLLECTION_ITEMS.name + " " +
            " LEFT JOIN SoundView ON(" +
            "   " + Table.COLLECTION_ITEMS.name + "." + CollectionItems.ITEM_ID + " = " + "SoundView." + SoundView._ID +
            " AND " + Table.COLLECTION_ITEMS.name + "." + CollectionItems.RESOURCE_TYPE + " = " + "SoundView." + SoundView._TYPE + ")" +
            " ORDER BY " + SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP + " DESC";

    /** A view which combines activity data + tracks/users/comments */
    static final String DATABASE_CREATE_ACTIVITY_VIEW = "AS SELECT " +
            "Activities." + Activities._ID + " as " + ActivityView._ID +
            ",Activities." + Activities.UUID + " as " + ActivityView.UUID+
            ",Activities." + Activities.TYPE + " as " + ActivityView.TYPE +
            ",Activities." + Activities.TAGS + " as " + ActivityView.TAGS+
            ",Activities." + Activities.CREATED_AT + " as " + ActivityView.CREATED_AT+
            ",Activities." + Activities.COMMENT_ID + " as " + ActivityView.COMMENT_ID+
            ",Activities." + Activities.SOUND_ID + " as " + ActivityView.SOUND_ID +
            ",Activities." + Activities.USER_ID + " as " + ActivityView.USER_ID +
            ",Activities." + Activities.CONTENT_ID + " as " + ActivityView.CONTENT_ID +
            ",Activities." + Activities.SHARING_NOTE_TEXT + " as " + ActivityView.SHARING_NOTE_TEXT +
            ",Activities." + Activities.SHARING_NOTE_CREATED_AT + " as " + ActivityView.SHARING_NOTE_CREATED_AT +


            // activity user (who commented, favorited etc. on contained following)
            ",Users." + Users.USERNAME + " as " + ActivityView.USER_USERNAME +
            ",Users." + Users.PERMALINK + " as " + ActivityView.USER_PERMALINK +
            ",Users." + Users.AVATAR_URL + " as " + ActivityView.USER_AVATAR_URL +

            // track+user data
            ",SoundView.*" +

            // comment data (only for type=comment)
            ",Comments." + Comments.BODY + " as " + ActivityView.COMMENT_BODY +
            ",Comments." + Comments.CREATED_AT + " as " + ActivityView.COMMENT_CREATED_AT +
            ",Comments." + Comments.TIMESTAMP + " as " +ActivityView.COMMENT_TIMESTAMP +
            " FROM Activities" +
            " LEFT JOIN Users ON(" +
            "   Activities." + Activities.USER_ID + " = " + "Users." + Users._ID + ")" +
            " LEFT JOIN SoundView ON(" +
            "   Activities." + Activities.SOUND_ID + " = " + "SoundView." + SoundView._ID + ")" +
            " LEFT JOIN Comments ON(" +
            "   Activities." + Activities.COMMENT_ID + " = " + "Comments." + Comments._ID + ")" +
            " ORDER BY " + ActivityView.CREATED_AT + " DESC"
            ;


    /**
     * {@link DBHelper.Suggestions}
     */
    static final String DATABASE_CREATE_SUGGESTIONS = "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "id  INTEGER," +
            "kind VARCHAR(32) NOT NULL," +
            "text VARCHAR(255) COLLATE NOCASE," +
            "icon_url       VARCHAR(255)," +
            "permalink_url  VARCHAR(255)," +
            "suggest_text_1 VARCHAR(255) NOT NULL," +
            "suggest_text_2 VARCHAR(255)," +
            "suggest_icon_1 VARCHAR(255)," +
            "suggest_intent_data VARCHAR(255)," +

            "UNIQUE(id, kind) ON CONFLICT REPLACE" +
            ")";

    static final String DATABASE_CREATE_CONNECTIONS = "("+
                "_id INTEGER PRIMARY KEY," +
                "user_id        INTEGER, "+
                "type           VARCHAR(255)," +
                "service        VARCHAR(255)," +
                "created_at     INTEGER," +
                "display_name   VARCHAR(255)," +
                "active         BOOLEAN DEFAULT 0, " +
                "post_publish   BOOLEAN DEFAULT 0, " +
                "post_like      BOOLEAN DEFAULT 0, " +
                "uri            VARCHAR(255)," +
                "UNIQUE (_id) ON CONFLICT REPLACE"+
                ");";

    public static class Connections implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String SERVICE = "service";
        public static final String TYPE = "type";
        public static final String CREATED_AT = "created_at";
        public static final String DISPLAY_NAME = "display_name";
        public static final String ACTIVE = "active";
        public static final String POST_PUBLISH = "post_publish";
        public static final String POST_LIKE = "post_like";
        public static final String URI = "uri";
    }

    public static class ResourceTable implements BaseColumns {
        public static final String _TYPE = "_type";
        public static final String CREATED_AT = "created_at";
        public static final String LAST_UPDATED = "last_updated";
        public static final String PERMALINK    = "permalink";
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_TRACKS}
     */
    public static class Sounds extends ResourceTable  {
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
        public static final String LIKES_COUNT = "favoritings_count";
        public static final String REPOSTS_COUNT = "reposts_count";
        public static final String SHARED_TO_COUNT = "shared_to_count";
        public static final String USER_ID = "user_id";
        public static final String STATE = "state";
        public static final String TRACKS_URI = "tracks_uri";
        public static final String PLAYLIST_TYPE = "playlist_type";

        public static final String[] ALL_FIELDS = {
                _ID, _TYPE, DURATION, TAG_LIST, TRACK_TYPE, TITLE, PERMALINK_URL, ARTWORK_URL,
                WAVEFORM_URL, DOWNLOADABLE, DOWNLOAD_URL, STREAM_URL, STREAM_URL,
                STREAMABLE, COMMENTABLE, SHARING, PLAYBACK_COUNT, DOWNLOAD_COUNT,
                COMMENT_COUNT, LIKES_COUNT, REPOSTS_COUNT, SHARED_TO_COUNT,
                USER_ID, STATE, CREATED_AT, PERMALINK, LAST_UPDATED, TRACKS_URI, PLAYLIST_TYPE
        };
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_TRACK_METADATA}
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
     * {@link DBHelper#DATABASE_CREATE_COLLECTION_ITEMS}
     */
    public static final class CollectionItems {
        public static final String ITEM_ID = "item_id";
        public static final String USER_ID = "user_id";     // "owner" of the item
        public static final String COLLECTION_TYPE = "collection_type"; // the association
        public static final String RESOURCE_TYPE = "resource_type";     // used by sounds to determine playlist or track
        public static final String POSITION = "position";
        public static final String SORT_ORDER = POSITION + " ASC";
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_USERS}
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
        public static final String PUBLIC_LIKES_COUNT = "public_favorites_count";
        public static final String PRIVATE_TRACKS_COUNT = "private_tracks_count";

        public static final String PLAN = "plan";

        public static final String[] ALL_FIELDS = {
                _ID, USERNAME, AVATAR_URL, CITY, COUNTRY, DISCOGS_NAME,
                FOLLOWERS_COUNT, FOLLOWINGS_COUNT, FULL_NAME, MYSPACE_NAME,
                TRACK_COUNT, WEBSITE, WEBSITE_TITLE, DESCRIPTION, PERMALINK,
                LAST_UPDATED, PERMALINK_URL, PRIMARY_EMAIL_CONFIRMED, PUBLIC_LIKES_COUNT,
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
     * {@link DBHelper#DATABASE_CREATE_RECORDINGS}
     */
    public static final class Recordings implements BaseColumns {
        public static final String USER_ID         = "user_id";
        public static final String TIMESTAMP       = "timestamp";
        public static final String LONGITUDE       = "longitude";
        public static final String LATITUDE        = "latitude";
        public static final String WHAT_TEXT       = "what_text";
        public static final String WHERE_TEXT      = "where_text";
        public static final String AUDIO_PATH      = "audio_path";
        public static final String DURATION        = "duration";
        public static final String DESCRIPTION     = "description";
        public static final String ARTWORK_PATH    = "artwork_path";
        public static final String SHARED_EMAILS   = "shared_emails";
        public static final String SHARED_IDS      = "shared_ids";
        public static final String PRIVATE_USER_ID = "private_user_id";
        public static final String TIP_KEY         = "tip_key";
        public static final String SERVICE_IDS     = "service_ids";
        public static final String IS_PRIVATE      = "is_private";
        public static final String EXTERNAL_UPLOAD = "external_upload";
        public static final String UPLOAD_STATUS   = "upload_status";
        public static final String FOUR_SQUARE_VENUE_ID = "four_square_venue_id";
        public static final String TRIM_LEFT       = "trim_left";
        public static final String TRIM_RIGHT      = "trim_right";
        public static final String FILTERS         = "filters";
        public static final String OPTIMIZE        = "optimize";
        public static final String FADING          = "fading";

        public static final String[] ALL_FIELDS = {
                _ID, USER_ID, TIMESTAMP, LONGITUDE, LATITUDE, WHAT_TEXT,
                WHERE_TEXT, AUDIO_PATH, DURATION, DESCRIPTION, ARTWORK_PATH,
                SHARED_EMAILS, SHARED_IDS, PRIVATE_USER_ID, TIP_KEY, SERVICE_IDS, IS_PRIVATE,
                EXTERNAL_UPLOAD, UPLOAD_STATUS, FOUR_SQUARE_VENUE_ID,
                TRIM_LEFT, TRIM_RIGHT, FILTERS, OPTIMIZE, FADING
        };
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_ACTIVITIES}
     */
    public static class Activities implements BaseColumns {
        public static final String UUID = "uuid";
        public static final String TYPE = "type";
        public static final String TAGS = "tags";
        public static final String USER_ID = "user_id";
        public static final String SOUND_ID = "track_id";
        public static final String COMMENT_ID = "comment_id";
        public static final String CREATED_AT  = "created_at";
        public static final String CONTENT_ID  = "content_id";
        public static final String SHARING_NOTE_TEXT = "sharing_note_text";
        public static final String SHARING_NOTE_CREATED_AT = "sharing_note_created_at";
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_SEARCHES}
     */
    public static final class Searches implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String SEARCH_TYPE = "search_type";
        public static final String CREATED_AT = "created_at";
        public static final String QUERY = "query";
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_COLLECTIONS}
     */
    public static final class Collections implements BaseColumns {
        public static final String URI = "uri";                      // local content provider uri
        public static final String LAST_ADDITION = "last_addition";  // last addition (from API, not used)
        public static final String LAST_SYNC = "last_sync";          // timestamp of last sync
        public static final String LAST_SYNC_ATTEMPT = "last_sync_attempt";          // timestamp of last sync
        public static final String SIZE = "size";
        public static final String SYNC_STATE = "sync_state";        // are we currently syncing?
        public static final String EXTRA = "extra";                  // general purpose field
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_COLLECTION_PAGES}
     */
    public static final class CollectionPages implements BaseColumns {
        public static final String COLLECTION_ID = "collection_id";
        public static final String ETAG = "etag";
        public static final String SIZE = "size";
        public static final String PAGE_INDEX = "page_index";
    }

    /**
     * {@link DBHelper#DATABASE_CREATE_PLAY_QUEUE}
     */
    public final static class PlayQueue implements BaseColumns{
        public static final String TRACK_ID = "track_id";
        public static final String POSITION = "position";
        public static final String USER_ID = "user_id";
    }

    public static class SoundView extends ResourceTable implements BaseColumns  {
        public static final String LAST_UPDATED = Sounds.LAST_UPDATED;
        public static final String PERMALINK = Sounds.PERMALINK;
        public static final String CREATED_AT = Sounds.CREATED_AT;
        public static final String DURATION = Sounds.DURATION;
        public static final String STATE = Sounds.STATE;
        public static final String TAG_LIST = Sounds.TAG_LIST;
        public static final String TRACK_TYPE = Sounds.TRACK_TYPE;
        public static final String TITLE = Sounds.TITLE;
        public static final String PERMALINK_URL = Sounds.PERMALINK_URL;
        public static final String ARTWORK_URL = Sounds.ARTWORK_URL;
        public static final String WAVEFORM_URL = Sounds.WAVEFORM_URL;
        public static final String DOWNLOADABLE = Sounds.DOWNLOADABLE;
        public static final String DOWNLOAD_URL = Sounds.DOWNLOAD_URL;
        public static final String STREAM_URL = Sounds.STREAM_URL;
        public static final String STREAMABLE = Sounds.STREAMABLE;
        public static final String COMMENTABLE = Sounds.COMMENTABLE;
        public static final String SHARING = Sounds.SHARING;
        public static final String PLAYBACK_COUNT = Sounds.PLAYBACK_COUNT;
        public static final String DOWNLOAD_COUNT = Sounds.DOWNLOAD_COUNT;
        public static final String COMMENT_COUNT = Sounds.COMMENT_COUNT;
        public static final String LIKES_COUNT = Sounds.LIKES_COUNT;
        public static final String REPOSTS_COUNT = Sounds.REPOSTS_COUNT;
        public static final String SHARED_TO_COUNT = Sounds.SHARED_TO_COUNT;

        public static final String USER_ID         = "sound_user_id";
        public static final String USERNAME        = "sound_user_username";
        public static final String USER_PERMALINK  = "sound_user_permalink";

        public static final String USER_AVATAR_URL = "sound_user_avatar_url";
        public static final String USER_LIKE       = "sound_user_like";
        public static final String USER_REPOST     = "sound_user_repost";
        public static final String USER_PLAY_COUNT = "sound_user_play_count";

        public static final String CACHED          = "sound_cached";
    }

    public final static class ActivityView extends Activities {
        public static final String COMMENT_BODY = "comment_body";
        public static final String COMMENT_TIMESTAMP = "comment_timestamp";
        public static final String COMMENT_CREATED_AT = "comment_created_at";

        public static final String USER_USERNAME = "activity_user_username";
        public static final String USER_PERMALINK = "activity_user_permalink";
        public static final String USER_AVATAR_URL = "activity_user_avatar_url";
    }

    public final static class SoundAssociationView extends SoundView {
        public static final String SOUND_ASSOCIATION_TIMESTAMP = "sound_association_timestamp";
        public static final String SOUND_ASSOCIATION_TYPE = "sound_association_type";
        public static final String SOUND_ASSOCIATION_USER_ID = "sound_association_user_id";
    }


    /**
     * @see <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     *  Building a suggestion table</a>
     */
    public final static class Suggestions implements BaseColumns {
        public static final String ID   = "id";

        // following | like | group
        public static final String KIND = "kind";

        // used as an index to search
        public static final String TEXT = "text";

        // avatar_url | artwork_url
        public static final String ICON_URL = "icon_url";

        // permalink_url
        public static final String PERMALINK_URL = "permalink_url";

        // use search manager compatible mappings
        public static final String COLUMN_TEXT1  = SearchManager.SUGGEST_COLUMN_TEXT_1;
        public static final String COLUMN_TEXT2  = SearchManager.SUGGEST_COLUMN_TEXT_2;
        public static final String COLUMN_ICON   = SearchManager.SUGGEST_COLUMN_ICON_1;

        // soundcloud:tracks:XXXX | soundcloud:users:XXXX
        public static final String INTENT_DATA   = SearchManager.SUGGEST_COLUMN_INTENT_DATA;

        public static final String[] ALL_FIELDS = {
            ID, KIND, TEXT, ICON_URL, PERMALINK_URL, COLUMN_TEXT1, COLUMN_TEXT2, COLUMN_ICON, INTENT_DATA
        };
    }

    /*
    * altered id naming for content resolver
    */
    private static boolean upgradeTo4(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db, new String[] { "id" }, new String[] { "_id" });
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
            Table.SOUNDS.alterColumns(db, null, null);
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
            Table.SOUNDS.alterColumns(db);
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
            Table.TRACK_PLAYS.drop(db);

            Table.SOUNDS.alterColumns(db);
            Table.USERS.alterColumns(db);

            // trackview refers to metadata now (http://www.bugsense.com/dashboard/project/806c72af#error/24301879)
            Table.TRACK_METADATA.create(db);

            Table.SOUND_VIEW.create(db);
            Table.COMMENTS.create(db);
            Table.ACTIVITIES.create(db);
            Table.ACTIVITY_VIEW.create(db);
            Table.COLLECTIONS.create(db);
            Table.COLLECTION_PAGES.create(db);
            Table.COLLECTION_ITEMS.create(db);
            Table.PLAY_QUEUE.create(db);
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
            Table.SOUND_VIEW.recreate(db);
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

    private static boolean upgradeTo13(SQLiteDatabase db, int oldVersion) {
            try {
                Table.SOUNDS.alterColumns(db);
                Table.SOUND_VIEW.recreate(db);
                Table.COLLECTIONS.alterColumns(db);
                Table.RECORDINGS.alterColumns(db);
                return true;
            } catch (SQLException e) {
                SoundCloudApplication.handleSilentException("error during upgrade13 " +
                        "(from " + oldVersion + ")", e);
            }
            return false;
        }

    private static boolean upgradeTo14(SQLiteDatabase db, int oldVersion) {
        try {
            resetSyncState(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade14 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo15(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade15 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo16(SQLiteDatabase db, int oldVersion) {
        try {
            // legacy tables
            db.execSQL("DROP TABLE IF EXISTS PlaylistItems");
            db.execSQL("DROP TABLE IF EXISTS Playlist");

            cleanActivities(db);
            Table.COLLECTION_ITEMS.alterColumns(db);
            Table.SOUNDS.alterColumns(db);
            Table.SOUND_VIEW.recreate(db);
            Table.ACTIVITY_VIEW.recreate(db);
            Table.PLAYLIST_TRACKS.create(db);
            Table.PLAY_QUEUE.create(db);
            Table.SOUND_ASSOCIATION_VIEW.create(db);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade16 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo17(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db);
            Table.ACTIVITIES.alterColumns(db);
            Table.SOUND_VIEW.recreate(db);
            Table.ACTIVITY_VIEW.recreate(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade17 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo18(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SUGGESTIONS.recreate(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade18 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /*
    Stream items upgrade
     */
    private static boolean upgradeTo19(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db);
            Table.ACTIVITIES.alterColumns(db);
            Table.SOUND_VIEW.recreate(db);
            Table.ACTIVITY_VIEW.recreate(db);
            Table.COLLECTION_ITEMS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade18 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    public static String getWhereInClause(String column, List<Long> idSet){
        StringBuilder sb = new StringBuilder(column + " IN (?");
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

    private static void resetSyncState(SQLiteDatabase db) {
        db.execSQL("UPDATE " + Table.COLLECTIONS + " SET " + Collections.SYNC_STATE + " =" + LocalCollection.SyncState.IDLE);
    }

}
