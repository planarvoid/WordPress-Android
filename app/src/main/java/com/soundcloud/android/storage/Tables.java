package com.soundcloud.android.storage;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.playlists.PlaylistQueries;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

public interface Tables {

    // Pure Tables
    class Sounds extends SCBaseTable {

        public static final Sounds TABLE = new Sounds();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column _TYPE = Column.create(TABLE, "_type", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class);
        public static final Column LAST_UPDATED = Column.create(TABLE, "last_updated", Long.class);
        public static final Column PERMALINK = Column.create(TABLE, "permalink", String.class);
        public static final Column ORIGINAL_CONTENT_SIZE = Column.create(TABLE, "original_content_size", Long.class);
        public static final Column GENRE = Column.create(TABLE, "genre", String.class);
        public static final Column DURATION = Column.create(TABLE, "duration", Long.class);
        public static final Column FULL_DURATION = Column.create(TABLE, "full_duration", Long.class);
        public static final Column SNIPPET_DURATION = Column.create(TABLE, "snippet_duration", Long.class);
        public static final Column TAG_LIST = Column.create(TABLE, "tag_list", String.class);
        public static final Column TRACK_TYPE = Column.create(TABLE, "track_type", String.class);
        public static final Column TITLE = Column.create(TABLE, "title", String.class);
        public static final Column PERMALINK_URL = Column.create(TABLE, "permalink_url", String.class);
        public static final Column ARTWORK_URL = Column.create(TABLE, "artwork_url", String.class);
        public static final Column WAVEFORM_URL = Column.create(TABLE, "waveform_url", String.class);
        public static final Column DOWNLOADABLE = Column.create(TABLE, "downloadable", Boolean.class);
        public static final Column DOWNLOAD_URL = Column.create(TABLE, "download_url", String.class);
        public static final Column STREAM_URL = Column.create(TABLE, "stream_url", String.class);
        public static final Column STREAMABLE = Column.create(TABLE, "streamable", Boolean.class);
        public static final Column COMMENTABLE = Column.create(TABLE, "commentable", Boolean.class);
        public static final Column SHARING = Column.create(TABLE, "sharing", String.class);
        public static final Column LICENSE = Column.create(TABLE, "license", String.class);
        public static final Column PURCHASE_URL = Column.create(TABLE, "purchase_url", String.class);
        public static final Column PLAYBACK_COUNT = Column.create(TABLE, "playback_count", Long.class);
        public static final Column DOWNLOAD_COUNT = Column.create(TABLE, "download_count", Long.class);
        public static final Column COMMENT_COUNT = Column.create(TABLE, "comment_count", Long.class);
        public static final Column LIKES_COUNT = Column.create(TABLE, "favoritings_count", Long.class);
        public static final Column REPOSTS_COUNT = Column.create(TABLE, "reposts_count", Long.class);
        public static final Column SHARED_TO_COUNT = Column.create(TABLE, "shared_to_count", Long.class);
        public static final Column USER_ID = Column.create(TABLE, "user_id", Long.class);
        public static final Column STATE = Column.create(TABLE, "state", String.class);
        public static final Column TRACKS_URI = Column.create(TABLE, "tracks_uri", String.class);
        public static final Column TRACK_COUNT = Column.create(TABLE, "track_count", Long.class);
        public static final Column PLAYLIST_TYPE = Column.create(TABLE, "playlist_type", String.class);
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class);
        public static final Column MODIFIED_AT = Column.create(TABLE, "modified_at", Long.class);
        public static final Column DESCRIPTION = Column.create(TABLE, "description", String.class);
        public static final Column IS_ALBUM = Column.create(TABLE, "is_album", Boolean.class);
        public static final Column SET_TYPE = Column.create(TABLE, "set_type", String.class);
        public static final Column RELEASE_DATE = Column.create(TABLE, "release_date", String.class);

        public static final int TYPE_TRACK = 0;
        public static final int TYPE_PLAYLIST = 1;
        public static final int TYPE_COLLECTION = 2;

        static final String SQL = "CREATE TABLE IF NOT EXISTS Sounds (" +
                "_id INTEGER," +
                "_type INTEGER," +
                "last_updated INTEGER," +
                "permalink VARCHAR(255)," +
                "original_content_size INTEGER," +
                "duration INTEGER," +
                "snippet_duration INTEGER," +
                "full_duration INTEGER," +
                "state VARCHAR(50)," +
                "created_at INTEGER," +
                "genre VARCHAR(100)," +
                "tag_list VARCHAR(500)," +
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
                "license VARCHAR(100)," +
                "purchase_url VARCHAR(255)," +
                "playback_count INTEGER DEFAULT -1," +
                "download_count INTEGER DEFAULT -1," +
                "comment_count INTEGER DEFAULT -1," +
                "favoritings_count INTEGER DEFAULT -1," +
                "reposts_count INTEGER DEFAULT -1," +
                "shared_to_count INTEGER DEFAULT -1," +
                "sharing_note_text VARCHAR(255)," +
                "tracks_uri VARCHAR(255)," +
                "track_count INTEGER DEFAULT -1," +
                "playlist_type VARCHAR(255)," +
                "user_id INTEGER," +
                "removed_at INTEGER DEFAULT NULL," +
                "modified_at INTEGER DEFAULT NULL," +
                "DESCRIPTION TEXT," +
                "is_album BOOLEAN DEFAULT 0," +
                "set_type VARCHAR(255)," +
                "release_date VARCHAR(255)," +
                "PRIMARY KEY (_id, _type) ON CONFLICT IGNORE" +
                ");";

        Sounds() {
            super("Sounds", PrimaryKey.of("_id", "_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class TrackPolicies extends SCBaseTable {

        public static final TrackPolicies TABLE = new TrackPolicies();

        public static final Column TRACK_ID = Column.create(TABLE, "track_id", Long.class);
        public static final Column MONETIZABLE = Column.create(TABLE, "monetizable", Boolean.class);
        public static final Column BLOCKED = Column.create(TABLE, "blocked", Boolean.class);
        public static final Column SNIPPED = Column.create(TABLE, "snipped", Boolean.class);
        public static final Column SUB_MID_TIER = Column.create(TABLE, "sub_mid_tier", Boolean.class);
        public static final Column SUB_HIGH_TIER = Column.create(TABLE, "sub_high_tier", Boolean.class);
        public static final Column POLICY = Column.create(TABLE, "policy", String.class);
        public static final Column MONETIZATION_MODEL = Column.create(TABLE, "monetization_model", String.class);
        public static final Column SYNCABLE = Column.create(TABLE, "syncable", Boolean.class);
        public static final Column LAST_UPDATED = Column.create(TABLE, "last_updated", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS TrackPolicies (" +
                "track_id INTEGER, " +
                "monetizable BOOLEAN DEFAULT 0," +
                "blocked BOOLEAN DEFAULT 0," +
                "snipped BOOLEAN DEFAULT 0," +
                "syncable BOOLEAN DEFAULT 1," +
                "sub_mid_tier BOOLEAN DEFAULT 0," +
                "sub_high_tier BOOLEAN DEFAULT 0," +
                "policy TEXT," +
                "monetization_model TEXT," +
                "last_updated INTEGER, " +
                "PRIMARY KEY (track_id) ON CONFLICT REPLACE " +
                ");";

        TrackPolicies() {
            super("TrackPolicies", PrimaryKey.of("track_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Users extends SCBaseTable {

        public static final Users TABLE = new Users();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column PERMALINK = Column.create(TABLE, "permalink", String.class);
        public static final Column USERNAME = Column.create(TABLE, "username", String.class);
        public static final Column AVATAR_URL = Column.create(TABLE, "avatar_url", String.class);
        public static final Column VISUAL_URL = Column.create(TABLE, "visual_url", String.class);
        public static final Column CITY = Column.create(TABLE, "city", String.class);
        public static final Column COUNTRY = Column.create(TABLE, "country", String.class);
        public static final Column DISCOGS_NAME = Column.create(TABLE, "discogs_name", String.class);
        public static final Column FOLLOWERS_COUNT = Column.create(TABLE, "followers_count", Long.class);
        public static final Column FOLLOWINGS_COUNT = Column.create(TABLE, "followings_count", Long.class);
        public static final Column FIRST_NAME = Column.create(TABLE, "first_name", String.class);
        public static final Column LAST_NAME = Column.create(TABLE, "last_name", String.class);
        public static final Column FULL_NAME = Column.create(TABLE, "full_name", String.class);
        public static final Column MYSPACE_NAME = Column.create(TABLE, "myspace_name", String.class);
        public static final Column TRACK_COUNT = Column.create(TABLE, "track_count", Long.class);
        public static final Column WEBSITE_URL = Column.create(TABLE, "website", String.class);
        public static final Column WEBSITE_NAME = Column.create(TABLE, "website_title", String.class);
        public static final Column DESCRIPTION = Column.create(TABLE, "description", String.class);
        public static final Column USER_FOLLOWING = Column.create(TABLE, "user_following", Boolean.class);
        public static final Column USER_FOLLOWER = Column.create(TABLE, "user_follower", Boolean.class);
        public static final Column PERMALINK_URL = Column.create(TABLE, "permalink_url", String.class);
        public static final Column ARTIST_STATION = Column.create(TABLE, "artist_station", String.class);
        public static final Column PRIMARY_EMAIL_CONFIRMED = Column.create(TABLE, "primary_email_confirmed", Boolean.class);
        public static final Column PUBLIC_LIKES_COUNT = Column.create(TABLE, "public_favorites_count", Long.class);
        public static final Column PRIVATE_TRACKS_COUNT = Column.create(TABLE, "private_tracks_count", Long.class);
        public static final Column SIGNUP_DATE = Column.create(TABLE, "signup_date", Long.class);

        public static final Column PLAN = Column.create(TABLE, "plan", String.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS Users (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "_type INTEGER DEFAULT 0," +

                // mini representation
                "username VARCHAR(255)," +
                "avatar_url VARCHAR(255)," +
                "permalink VARCHAR(255)," +
                "permalink_url VARCHAR(255)," +

                "first_name VARCHAR(255)," +
                "last_name VARCHAR(255)," +
                "full_name VARCHAR(255)," +
                "description text," +
                "city VARCHAR(255)," +
                "country VARCHAR(255)," +

                "artist_station TEXT," +
                "plan VARCHAR(16)," +
                "primary_email_confirmed INTEGER," +

                "website VARCHAR(255)," +
                "website_title VARCHAR(255), " +

                "discogs_name VARCHAR(255)," +
                "myspace_name VARCHAR(255)," +

                "visual_url VARCHAR(255)," +

                // counts
                "track_count INTEGER DEFAULT -1," +
                "followers_count INTEGER DEFAULT -1," +
                "followings_count INTEGER DEFAULT -1," +
                "public_favorites_count INTEGER DEFAULT -1," +
                "private_tracks_count INTEGER DEFAULT -1," +
                "signup_date INTEGER DEFAULT -1," +

                // internal
                "last_updated INTEGER" +
                ");";

        Users() {
            super("Users", PrimaryKey.of("_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class UserAssociations extends SCBaseTable {

        public static final UserAssociations TABLE = new UserAssociations();

        public static final Column TARGET_ID = Column.create(TABLE, "target_id", Long.class);// the target user of the association
        public static final Column ASSOCIATION_TYPE = Column.create(TABLE, "association_type", Long.class); // the type of association (e.g. Following, Follower)
        @Deprecated // we only store followings now
        public static final Column RESOURCE_TYPE = Column.create(TABLE, "resource_type", Long.class); // the type of resource (e.g. Folloiwng / follower)
        public static final Column POSITION = Column.create(TABLE, "position", Long.class); // as returned from the api
        @Deprecated public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class); // indicates when this was created on the api
        public static final Column ADDED_AT = Column.create(TABLE, "added_at", Long.class); // when was this added locally (pre-api sync)
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class); // when was this removed locally (pre-api sync)
        @Deprecated public static final Column TOKEN = Column.create(TABLE, "token", String.class);

        public static final int TYPE_FOLLOWING = 2; // inlined from ScContentProvider
        public static final int TYPE_FOLLOWER = 3; // inlined from ScContentProvider

        public static final int TYPE_RESOURCE_USER = 0;

        static final String SQL = "CREATE TABLE IF NOT EXISTS UserAssociations (" +
                "target_id INTEGER," +                  // the target user of the association
                "association_type INTEGER, " +          // the type of association (e.g. Following, Follower)
                "resource_type INTEGER DEFAULT 0, " +   // currently unused, but if we add groups...
                "position INTEGER, " +                  // as returned from the api
                "created_at INTEGER, " +                // indicates when this was created on the api
                "added_at INTEGER, " +                  // when was this added locally (pre-api sync)
                "removed_at INTEGER, " +                // when was this removed locally (pre-api sync)
                "token VARCHAR(150), " +                // whitelist token to avoid spam flagging. comes from API
                "PRIMARY KEY(target_id, association_type, resource_type) ON CONFLICT REPLACE" +
                ");";

        UserAssociations() {
            super("UserAssociations", PrimaryKey.of("target_id", "association_type", "resource_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Posts extends SCBaseTable {

        public static final Posts TABLE = new Posts();

        public static final Column TYPE = Column.create(TABLE, "type", String.class);
        public static final Column TARGET_TYPE = Column.create(TABLE, "target_type", Long.class);
        public static final Column TARGET_ID = Column.create(TABLE, "target_id", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class);

        /* not used (yet) */
        public static final Column ADDED_AT = Column.create(TABLE, "added_at", Long.class); // local addition, Long.class)s
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class); // local removal, Long.class)s

        public static final String TYPE_POST = "post";
        public static final String TYPE_REPOST = "repost";

        static final String SQL = "CREATE TABLE IF NOT EXISTS Posts (" +
                "type STRING NOT NULL," +
                "target_id INTEGER NOT NULL," +
                "target_type INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "added_at INTEGER DEFAULT NULL," +
                "removed_at INTEGER DEFAULT NULL," +
                "PRIMARY KEY (type, target_id, target_type)," +
                "FOREIGN KEY(target_id, target_type) REFERENCES Sounds(_id, _type)" +
                ");";

        Posts() {
            super("Posts", PrimaryKey.of("type", "target_type", "target_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }



    class Likes extends SCBaseTable {

        public static final Likes TABLE = new Likes();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column _TYPE = Column.create(TABLE, "_type", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class);
        public static final Column ADDED_AT = Column.create(TABLE, "added_at", Long.class);
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS Likes (" +
                "_id INTEGER NOT NULL," +
                "_type INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "added_at INTEGER DEFAULT NULL," +
                "removed_at INTEGER DEFAULT NULL," +
                "PRIMARY KEY (_id, _type)," +
                "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
                ");";

        Likes() {
            super("Likes", PrimaryKey.of("_id", "_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Recommendations extends SCBaseTable {

        // table instance
        public static final Recommendations TABLE = new Recommendations();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column SEED_ID = Column.create(TABLE, "seed_id", Long.class);
        public static final Column RECOMMENDED_SOUND_ID = Column.create(TABLE, "recommended_sound_id", Long.class);
        public static final Column RECOMMENDED_SOUND_TYPE = Column.create(TABLE, "recommended_sound_type", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS Recommendations (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_id INTEGER, " +
                "recommended_sound_id INTEGER," +
                "recommended_sound_type INTEGER," +
                "FOREIGN KEY(seed_id) REFERENCES RecommendationSeeds(_id) " +
                "FOREIGN KEY(recommended_sound_id, recommended_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        Recommendations() {
            super("Recommendations", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecommendationSeeds extends SCBaseTable {

        // table instance
        public static final RecommendationSeeds TABLE = new RecommendationSeeds();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column SEED_SOUND_ID = Column.create(TABLE, "seed_sound_id", Long.class);
        public static final Column SEED_SOUND_TYPE = Column.create(TABLE, "seed_sound_type", Long.class);
        public static final Column RECOMMENDATION_REASON = Column.create(TABLE, "recommendation_reason", Long.class);
        public static final Column QUERY_POSITION = Column.create(TABLE, "query_position", Long.class);
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn", String.class);

        public static final int REASON_LIKED = 0;
        public static final int REASON_PLAYED = 1;

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecommendationSeeds (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_sound_id INTEGER, " +
                "seed_sound_type INTEGER, " +
                "recommendation_reason INTEGER, " +
                "query_position INTEGER, " +
                "query_urn TEXT, " +
                "FOREIGN KEY(seed_sound_id, seed_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        RecommendationSeeds() {
            super("RecommendationSeeds", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlayQueue extends SCBaseTable {

        public static final PlayQueue TABLE = new PlayQueue();

        public static final Column ENTITY_ID = Column.create(TABLE, "entity_id", Long.class);
        public static final Column ENTITY_TYPE = Column.create(TABLE, "entity_type", Long.class);
        public static final Column REPOSTER_ID = Column.create(TABLE, "reposter_id", Long.class);
        public static final Column RELATED_ENTITY = Column.create(TABLE, "related_entity", String.class);
        public static final Column SOURCE = Column.create(TABLE, "source", String.class);
        public static final Column SOURCE_VERSION = Column.create(TABLE, "source_version", String.class);
        public static final Column SOURCE_URN = Column.create(TABLE, "source_urn", String.class);
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn", String.class);
        public static final Column CONTEXT_TYPE = Column.create(TABLE, "context_type", String.class);
        public static final Column CONTEXT_URN = Column.create(TABLE, "context_urn", String.class);
        public static final Column CONTEXT_QUERY = Column.create(TABLE, "context_query", String.class);
        public static final Column PLAYED = Column.create(TABLE, "played", Boolean.class);

        public static final int ENTITY_TYPE_TRACK = 0;
        public static final int ENTITY_TYPE_PLAYLIST = 1;

        static final String SQL = "CREATE TABLE IF NOT EXISTS PlayQueue (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "entity_id INTEGER," +
                "entity_type INTEGER," +
                "reposter_id INTEGER," +
                "related_entity TEXT," +
                "source TEXT," +
                "source_version TEXT," +
                "source_urn TEXT," +
                "query_urn TEXT," +
                "context_type TEXT," +
                "context_urn TEXT," +
                "context_query TEXT," +
                "played BOOLEAN default 1" +
                ");";

        PlayQueue() {
            super("PlayQueue", PrimaryKey.of(_ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Stations extends SCBaseTable {
        public static final Stations TABLE = new Stations();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn", String.class);
        public static final Column TYPE = Column.create(TABLE, "type", String.class);
        public static final Column TITLE = Column.create(TABLE, "title", String.class);
        public static final Column PERMALINK = Column.create(TABLE, "permalink", String.class);
        public static final Column ARTWORK_URL_TEMPLATE = Column.create(TABLE, "artwork_url_template", String.class);
        public static final Column LAST_PLAYED_TRACK_POSITION = Column.create(TABLE, "last_played_track_position", Long.class);
        public static final Column PLAY_QUEUE_UPDATED_AT = Column.create(TABLE, "play_queue_updated_at", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS Stations (" +
                "station_urn TEXT," +
                "type TEXT," +
                "title TEXT," +
                "permalink TEXT," +
                "artwork_url_template TEXT," +
                "last_played_track_position INTEGER DEFAULT NULL," +
                "play_queue_updated_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY(station_urn) ON CONFLICT REPLACE" +
                ");";

        Stations() {
            super("Stations", PrimaryKey.of("station_urn"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class StationsPlayQueues extends SCBaseTable {
        public static final StationsPlayQueues TABLE = new StationsPlayQueues();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn", String.class);
        public static final Column TRACK_ID = Column.create(TABLE, "track_id", Long.class);
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn", String.class);
        public static final Column POSITION = Column.create(TABLE, "position", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsPlayQueues (" +
                "station_urn TEXT," +
                "track_id INTEGER," +
                "query_urn TEXT," +
                "position INTEGER DEFAULT 0," +
                "PRIMARY KEY(station_urn, track_id, position) ON CONFLICT REPLACE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        StationsPlayQueues() {
            super("StationsPlayQueues", PrimaryKey.of("station_urn", "track_id", "position"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class StationsCollections extends SCBaseTable {
        public static final StationsCollections TABLE = new StationsCollections();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn", String.class);
        public static final Column COLLECTION_TYPE = Column.create(TABLE, "collection_type", Long.class);
        public static final Column POSITION = Column.create(TABLE, "position", Long.class);
        public static final Column ADDED_AT = Column.create(TABLE, "added_at", Long.class);
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsCollections (" +
                "station_urn TEXT NOT NULL," +
                "collection_type INTEGER NOT NULL," +
                "position INTEGER," +
                "added_at INTEGER," +
                "removed_at INTEGER," +
                "PRIMARY KEY(station_urn, collection_type) ON CONFLICT IGNORE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        StationsCollections() {
            super("StationsCollections", PrimaryKey.of("station_urn, collection_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class TrackDownloads extends SCBaseTable {

        public static final TrackDownloads TABLE = new TrackDownloads();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at", Long.class);
        public static final Column REQUESTED_AT = Column.create(TABLE, "requested_at", Long.class);
        public static final Column DOWNLOADED_AT = Column.create(TABLE, "downloaded_at", Long.class);
        public static final Column UNAVAILABLE_AT = Column.create(TABLE, "unavailable_at", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS TrackDownloads (" +
                "_id INTEGER PRIMARY KEY," +
                "requested_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                "downloaded_at INTEGER DEFAULT NULL," +
                "removed_at INTEGER DEFAULT NULL," + // track marked for deletion
                "unavailable_at INTEGER DEFAULT NULL" +
                ");";

        TrackDownloads() {
            super("TrackDownloads", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class OfflineContent extends SCBaseTable {

        public static final OfflineContent TABLE = new OfflineContent();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column _TYPE = Column.create(TABLE, "_type", Long.class);

        public static final int TYPE_PLAYLIST = Sounds.TYPE_PLAYLIST;
        public static final int TYPE_COLLECTION = Sounds.TYPE_COLLECTION;

        public static final int ID_OFFLINE_LIKES = 0;

        static final String SQL = "CREATE TABLE IF NOT EXISTS OfflineContent (" +
                "_id INTEGER," +
                "_type INTEGER," +
                "PRIMARY KEY (_id, _type)," +
                "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
                ");";

        OfflineContent() {
            super("OfflineContent", PrimaryKey.of(BaseColumns._ID, "_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Comments extends SCBaseTable {

        public static final Comments TABLE = new Comments();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID, Long.class);
        public static final Column URN = Column.create(TABLE, "urn", String.class);
        public static final Column USER_ID = Column.create(TABLE, "user_id", Long.class);
        public static final Column TRACK_ID = Column.create(TABLE, "track_id", Long.class);
        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class);
        public static final Column BODY = Column.create(TABLE, "body", String.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS Comments (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "urn TEXT UNIQUE," +
                "user_id INTEGER," +
                "track_id INTEGER," +
                "timestamp INTEGER," +
                "created_at INTEGER," +
                "body VARCHAR(255)" +
                ");";

        Comments() {
            super("Comments", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Charts extends SCBaseTable {

        // table instance
        public static final Charts TABLE = new Charts();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column DISPLAY_NAME = Column.create(TABLE, "display_name", String.class);
        public static final Column GENRE = Column.create(TABLE, "genre", String.class);
        public static final Column TYPE = Column.create(TABLE, "type", String.class);
        public static final Column CATEGORY = Column.create(TABLE, "category", String.class);
        public static final Column BUCKET_TYPE = Column.create(TABLE, "bucket_type", Long.class);

        public static final int BUCKET_TYPE_GLOBAL = 0;
        public static final int BUCKET_TYPE_FEATURED_GENRES = 1;
        public static final int BUCKET_TYPE_ALL_GENRES = 2;

        static final String SQL = "CREATE TABLE IF NOT EXISTS Charts (" +
                "_id INTEGER PRIMARY KEY," +
                "display_name TEXT, " +
                "genre TEXT, " +
                "type TEXT, " +
                "category TEXT," +
                "bucket_type INTEGER" +
                ");";

        Charts() {
            super("Charts", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class ChartTracks extends SCBaseTable {

        // table instance
        public static final ChartTracks TABLE = new ChartTracks();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column CHART_ID = Column.create(TABLE, "chart_id", Long.class);
        public static final Column TRACK_ID = Column.create(TABLE, "track_id", Long.class);
        public static final Column TRACK_ARTWORK = Column.create(TABLE, "track_artwork", String.class);
        public static final Column BUCKET_TYPE = Column.create(TABLE, "bucket_type", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS ChartTracks (" +
                "_id INTEGER PRIMARY KEY," +
                "chart_id INTEGER, " +
                "track_id INTEGER, " +
                "track_artwork TEXT, " +
                "bucket_type INTEGER," +
                "FOREIGN KEY(chart_id) REFERENCES Charts(_id) " +
                ");";

        ChartTracks() {
            super("ChartTracks", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlayHistory extends SCBaseTable {
        public static final PlayHistory TABLE = new PlayHistory();

        public static final Column TRACK_ID = Column.create(TABLE, "track_id", Long.class);
        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp", Long.class);
        public static final Column SYNCED = Column.create(TABLE, "synced", Boolean.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS PlayHistory (" +
                "timestamp INTEGER NOT NULL," +
                "track_id INTEGER NOT NULL," +
                "synced BOOLEAN DEFAULT 0," +
                "PRIMARY KEY (timestamp, track_id)" +
                ");";

        PlayHistory() {
            super("PlayHistory", PrimaryKey.of("timestamp", "track_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecentlyPlayed extends SCBaseTable {
        public static final RecentlyPlayed TABLE = new RecentlyPlayed();

        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp", Long.class);
        public static final Column CONTEXT_TYPE = Column.create(TABLE, "context_type", Long.class);
        public static final Column CONTEXT_ID = Column.create(TABLE, "context_id", Long.class);
        public static final Column SYNCED = Column.create(TABLE, "synced", Boolean.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecentlyPlayed (" +
                "timestamp INTEGER NOT NULL," +
                "context_type INTEGER NOT NULL," +
                "context_id INTEGER NOT NULL," +
                "synced BOOLEAN DEFAULT 0," +
                "PRIMARY KEY (timestamp, context_type, context_id)" +
                ");";

        static final String MIGRATE_SQL = "INSERT OR IGNORE INTO RecentlyPlayed " +
                "(timestamp, context_type, context_id) " +
                "SELECT timestamp, context_type, context_id " +
                "FROM PlayHistory WHERE context_type != 0;";

        RecentlyPlayed() {
            super("RecentlyPlayed", PrimaryKey.of("timestamp", "context_type", "context_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class SuggestedCreators extends SCBaseTable {
        public static final SuggestedCreators TABLE = new SuggestedCreators();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column SEED_USER_ID = Column.create(TABLE, "seed_user_id", Long.class);
        public static final Column SUGGESTED_USER_ID = Column.create(TABLE, "suggested_user_id", Long.class);
        public static final Column RELATION_KEY = Column.create(TABLE, "relation_key", String.class);
        public static final Column FOLLOWED_AT = Column.create(TABLE, "followed_at", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS SuggestedCreators (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "seed_user_id INTEGER, " +
                "suggested_user_id INTEGER, " +
                "relation_key VARCHAR(50), " +
                "followed_at INTEGER," +
                "FOREIGN KEY(seed_user_id) REFERENCES Users(_id) " +
                "FOREIGN KEY(suggested_user_id) REFERENCES Users(_id) " +
                ");";

        SuggestedCreators() {
            super("SuggestedCreators", PrimaryKey.of("_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    // Views

    class SearchSuggestions extends SCBaseTable {

        public static final SearchSuggestions TABLE = new SearchSuggestions();

        public static final Column KIND = Column.create(TABLE, "kind", String.class);
        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column _TYPE = Column.create(TABLE, "_type", Long.class);
        public static final Column DISPLAY_TEXT = Column.create(TABLE, "display_text", String.class);
        public static final Column IMAGE_URL = Column.create(TABLE, "image_url", String.class);

        public static final String KIND_LIKE = "like";
        public static final String KIND_FOLLOWING = "following";

        static final String SQL = "CREATE VIEW IF NOT EXISTS SearchSuggestions AS SELECT '" +
                KIND_LIKE + "' AS kind, " +
                "Sounds._id AS _id, " +
                "Sounds._type AS _type, " +
                "title AS display_text, " +
                "artwork_url AS image_url " +
                "FROM Likes INNER JOIN Sounds ON Likes._id = Sounds._id " +
                "AND Likes._type = Sounds._type" +
                " UNION" +
                " SELECT '" + KIND_FOLLOWING + "' AS kind, " +
                "Users._id, 0 AS _type, " +
                "username AS text, " +
                "avatar_url AS image_url " +
                "from UserAssociations INNER JOIN Users ON UserAssociations.target_id = Users._id";

        SearchSuggestions() {
            super("SearchSuggestions", PrimaryKey.of(BaseColumns._ID, "kind"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class OfflinePlaylistTracks extends SCBaseTable {

        public static final OfflinePlaylistTracks TABLE = new OfflinePlaylistTracks();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID, Long.class);
        public static final Column _TYPE = Column.create(TABLE, "_type", Long.class);
        public static final Column USER_ID = Column.create(TABLE, "user_id", Long.class);
        public static final Column DURATION = Column.create(TABLE, "duration", Long.class);
        public static final Column WAVEFORM_URL = Column.create(TABLE, "waveform_url", String.class);
        public static final Column ARTWORK_URL = Column.create(TABLE, "artwork_url", String.class);
        public static final Column SYNCABLE = Column.create(TABLE, "syncable", Boolean.class);
        public static final Column SNIPPED = Column.create(TABLE, "snipped", Boolean.class);
        public static final Column LAST_POLICY_UPDATE = Column.create(TABLE, "last_policy_update", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "created_at", Long.class);
        public static final Column POSITION = Column.create(TABLE, "position", Long.class);

        static final String SQL = "CREATE VIEW IF NOT EXISTS OfflinePlaylistTracks AS " +
                "SELECT " +
                "Sounds._id as _id," +
                "Sounds._type as _type, " +
                "Sounds.user_id as user_id, " +
                "Sounds.full_duration as duration, " +
                "Sounds.waveform_url as waveform_url, " +
                "Sounds.artwork_url as artwork_url, " +
                "TrackPolicies.syncable as syncable, " +
                "TrackPolicies.snipped as snipped, " +
                "TrackPolicies.last_updated as last_policy_update, " +
                "PlaylistTracks.position as position, " +
                "MAX(IFNULL(PlaylistLikes.created_at, 0), PlaylistProperties.created_at ) AS created_at " +
                // ^ The timestamp used to sort
                "FROM Sounds " +
                "INNER JOIN PlaylistTracks ON Sounds._id = PlaylistTracks.track_id " +
                // ^ Add PlaylistTracks to tracks
                "LEFT JOIN Likes as PlaylistLikes ON (PlaylistTracks.playlist_id = PlaylistLikes._id) AND (PlaylistLikes._type = " + Sounds.TYPE_PLAYLIST + ") " +
                // ^ When available, adds the Playlist Like date to the tracks (for sorting purpose)
                "LEFT JOIN Sounds as PlaylistProperties ON (PlaylistProperties._id = PlaylistTracks.playlist_id AND PlaylistProperties._type = " + Sounds.TYPE_PLAYLIST + ")" +
                // ^ Add the playlist creation date
                "INNER JOIN OfflineContent ON PlaylistTracks.playlist_id = OfflineContent._id  AND Sounds._type = " + TYPE_TRACK + " " +
                // ^ Keep only offline tracks
                "INNER JOIN TrackPolicies ON PlaylistTracks.track_id = TrackPolicies.track_id " +
                // ^ Keep only tracks with policies
                "WHERE (PlaylistTracks.removed_at IS NULL) ";

        OfflinePlaylistTracks() {
            super("OfflinePlaylistTracks", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlaylistView extends SCBaseTable {

        public static final PlaylistView TABLE = new PlaylistView();

        PlaylistView() {
            super("PlaylistView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "pv_id", Long.class);
        public static final Column TITLE = Column.create(TABLE, "pv_title", String.class);
        public static final Column USERNAME = Column.create(TABLE, "pv_username", String.class);
        public static final Column USER_ID = Column.create(TABLE, "pv_user_id", Long.class);
        public static final Column TRACK_COUNT = Column.create(TABLE, "pv_track_count", Long.class);
        public static final Column DURATION = Column.create(TABLE, "pv_duration", Long.class);
        public static final Column LIKES_COUNT = Column.create(TABLE, "pv_likes_count", Long.class);
        public static final Column REPOSTS_COUNT = Column.create(TABLE, "pv_reposts_count", Long.class);
        public static final Column SHARING = Column.create(TABLE, "pv_sharing", String.class);
        public static final Column ARTWORK_URL = Column.create(TABLE, "pv_artwork_url", String.class);
        public static final Column PERMALINK_URL = Column.create(TABLE, "pv_permalink_url", String.class);
        public static final Column GENRE = Column.create(TABLE, "pv_genre", String.class);
        public static final Column TAG_LIST = Column.create(TABLE, "pv_tag_list", String.class);
        public static final Column CREATED_AT = Column.create(TABLE, "pv_created_at", Long.class);
        public static final Column RELEASE_DATE = Column.create(TABLE, "pv_release_date", String.class);
        public static final Column SET_TYPE = Column.create(TABLE, "pv_set_type", String.class);
        public static final Column LOCAL_TRACK_COUNT = Column.create(TABLE, "pv_local_track_count", Long.class);
        public static final Column HAS_PENDING_DOWNLOAD_REQUEST = Column.create(TABLE,
                                                                                "pv_has_pending_download_request", Boolean.class);
        public static final Column HAS_DOWNLOADED_TRACKS = Column.create(TABLE, "pv_has_downloaded_tracks", Boolean.class);
        public static final Column HAS_UNAVAILABLE_TRACKS = Column.create(TABLE, "pv_has_unavailable_tracks", Boolean.class);
        public static final Column IS_MARKED_FOR_OFFLINE = Column.create(TABLE, "pv_is_marked_for_offline", Boolean.class);
        public static final Column IS_USER_LIKE = Column.create(TABLE, "pv_is_user_like", Boolean.class);
        public static final Column IS_USER_REPOST = Column.create(TABLE, "pv_is_user_repost", Boolean.class);
        public static final Column IS_ALBUM = Column.create(TABLE, "pv_is_album", Boolean.class);

        static final String SQL = "CREATE VIEW IF NOT EXISTS PlaylistView AS " +
                Query.from(SoundView.name())
                     .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                             field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                             field(SoundView.field(TableColumns.SoundView.USERNAME)).as(USERNAME.name()),
                             field(SoundView.field(TableColumns.SoundView.USER_ID)).as(USER_ID.name()),
                             field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TRACK_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.DURATION)).as(DURATION.name()),
                             field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),
                             field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                             field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),
                             field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.RELEASE_DATE)).as(RELEASE_DATE.name()),
                             field(SoundView.field(TableColumns.SoundView.SET_TYPE)).as(SET_TYPE.name()),
                             field(SoundView.field(TableColumns.SoundView.IS_ALBUM)).as(IS_ALBUM.name()),
                             field("(" + PlaylistQueries.LOCAL_TRACK_COUNT.build() + ")").as(LOCAL_TRACK_COUNT.name()),
                             exists(likeQuery()).as(IS_USER_LIKE.name()),
                             exists(repostQuery()).as(IS_USER_REPOST.name()),
                             exists(PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(HAS_PENDING_DOWNLOAD_REQUEST.name()),
                             exists(PlaylistQueries.HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER).as(HAS_DOWNLOADED_TRACKS.name()),
                             exists(PlaylistQueries.HAS_UNAVAILABLE_OFFLINE_TRACKS_FILTER).as(HAS_UNAVAILABLE_TRACKS.name()),
                             exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(IS_MARKED_FOR_OFFLINE.name()))
                     .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Sounds.TYPE_PLAYLIST);

        static Query likeQuery() {
            final Where joinConditions = filter()
                    .whereEq(Table.SoundView.field(TableColumns.SoundView._ID), Tables.Likes._ID)
                    .whereEq(Table.SoundView.field(TableColumns.SoundView._TYPE), Tables.Likes._TYPE);

            return Query.from(Tables.Likes.TABLE)
                        // do not use SoundView here. The exists query will fail, in spite of passing tests
                        .innerJoin(Sounds.TABLE, joinConditions)
                        .whereNull(Tables.Likes.REMOVED_AT);
        }

        static Query repostQuery() {
            final Where joinConditions = filter()
                    .whereEq(Table.SoundView.field(TableColumns.SoundView._ID), Posts.TARGET_ID)
                    .whereEq(Table.SoundView.field(TableColumns.SoundView._TYPE), Posts.TARGET_TYPE);

            return Query.from(Posts.TABLE)
                        .innerJoin(Sounds.TABLE, joinConditions)
                        .whereEq(Tables.Sounds._TYPE.qualifiedName(), Sounds.TYPE_PLAYLIST)
                        .whereEq(Posts.TYPE.qualifiedName(), typeRepostDelimited());
        }

        private static String typeRepostDelimited() {
            return "'" + Posts.TYPE_REPOST + "'";
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class UsersView extends SCBaseTable {

        public static final UsersView TABLE = new UsersView();

        UsersView() {
            super("UsersView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "uv_id", Long.class);
        public static final Column USERNAME = Column.create(TABLE, "uv_username", String.class);
        public static final Column FIRST_NAME = Column.create(TABLE, "uv_first_name", String.class);
        public static final Column LAST_NAME = Column.create(TABLE, "uv_last_name", String.class);
        public static final Column COUNTRY = Column.create(TABLE, "uv_country", String.class);
        public static final Column CITY = Column.create(TABLE, "uv_city", String.class);
        public static final Column FOLLOWERS_COUNT = Column.create(TABLE, "uv_followers_count", Long.class);
        public static final Column FOLLOWINGS_COUNT = Column.create(TABLE, "uv_followings_count", Long.class);
        public static final Column DESCRIPTION = Column.create(TABLE, "uv_description", String.class);
        public static final Column AVATAR_URL = Column.create(TABLE, "uv_avatar_url", String.class);
        public static final Column VISUAL_URL = Column.create(TABLE, "uv_visual_url", String.class);
        public static final Column WEBSITE_URL = Column.create(TABLE, "uv_website_url", String.class);
        public static final Column WEBSITE_NAME = Column.create(TABLE, "uv_website_name", String.class);
        public static final Column MYSPACE_NAME = Column.create(TABLE, "uv_myspace_name", String.class);
        public static final Column DISCOGS_NAME = Column.create(TABLE, "uv_discogs_name", String.class);
        public static final Column ARTIST_STATION = Column.create(TABLE, "uv_artist_station", String.class);
        public static final Column IS_FOLLOWING = Column.create(TABLE, "uv_is_following", Boolean.class);
        public static final Column SIGNUP_DATE = Column.create(TABLE, "uv_signup_date", String.class);


        static final String SQL = "CREATE VIEW IF NOT EXISTS UsersView AS " +
                Query.from(Users.TABLE)
                     .select(
                             Users._ID.as(ID.name()),
                             Users.USERNAME.as(USERNAME.name()),
                             Users.FIRST_NAME.as(FIRST_NAME.name()),
                             Users.LAST_NAME.as(LAST_NAME.name()),
                             Users.COUNTRY.as(COUNTRY.name()),
                             Users.CITY.as(CITY.name()),
                             Users.FOLLOWERS_COUNT.as(FOLLOWERS_COUNT.name()),
                             Users.FOLLOWINGS_COUNT.as(FOLLOWINGS_COUNT.name()),
                             Users.DESCRIPTION.as(DESCRIPTION.name()),
                             Users.AVATAR_URL.as(AVATAR_URL.name()),
                             Users.VISUAL_URL.as(VISUAL_URL.name()),
                             Users.WEBSITE_URL.as(WEBSITE_URL.name()),
                             Users.WEBSITE_NAME.as(WEBSITE_NAME.name()),
                             Users.MYSPACE_NAME.as(MYSPACE_NAME.name()),
                             Users.DISCOGS_NAME.as(DISCOGS_NAME.name()),
                             Users.ARTIST_STATION.as(ARTIST_STATION.name()),
                             exists(followingQuery()).as(IS_FOLLOWING.name()),
                             Users.SIGNUP_DATE.as(SIGNUP_DATE.name())
                     );

        static Query followingQuery() {
            return Query.from(UserAssociations.TABLE)
                        .whereEq(Users._ID,
                                 UserAssociations.TARGET_ID)
                        .whereNull(Tables.UserAssociations.REMOVED_AT);
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class TrackView extends SCBaseTable {

        public static final TrackView TABLE = new TrackView();

        TrackView() {
            super("TrackView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "tv_id", Long.class);
        public static final Column CREATED_AT = Column.create(TABLE, "tv_created_at", Long.class);
        public static final Column TITLE = Column.create(TABLE, "tv_title", String.class);
        public static final Column CREATOR_NAME = Column.create(TABLE, "tv_username", String.class);
        public static final Column CREATOR_ID = Column.create(TABLE, "tv_user_id", Long.class);
        public static final Column PERMALINK_URL = Column.create(TABLE, "tv_permalink_url", String.class);
        public static final Column WAVEFORM_URL = Column.create(TABLE, "tv_waveform_url", String.class);

        public static final Column SNIPPET_DURATION = Column.create(TABLE, "tv_snippet_duration", Long.class);
        public static final Column FULL_DURATION = Column.create(TABLE, "tv_full_duration", Long.class);

        public static final Column PLAY_COUNT = Column.create(TABLE, "tv_play_count", Long.class);
        public static final Column LIKES_COUNT = Column.create(TABLE, "tv_likes_count", Long.class);
        public static final Column REPOSTS_COUNT = Column.create(TABLE, "tv_reposts_count", Long.class);
        public static final Column COMMENTS_COUNT = Column.create(TABLE, "tv_comments_count", Long.class);
        public static final Column IS_COMMENTABLE = Column.create(TABLE, "tv_is_commentable", Boolean.class);
        public static final Column GENRE = Column.create(TABLE, "tv_genre", String.class);
        public static final Column TAG_LIST = Column.create(TABLE, "tv_tag_list", String.class);
        public static final Column SHARING = Column.create(TABLE, "tv_sharing", String.class);
        public static final Column POLICY = Column.create(TABLE, "tv_policy", String.class);
        public static final Column MONETIZABLE = Column.create(TABLE, "tv_monetizable", Boolean.class);
        public static final Column MONETIZATION_MODEL = Column.create(TABLE, "tv_monetization_model", String.class);
        public static final Column BLOCKED = Column.create(TABLE, "tv_blocked", Boolean.class);
        public static final Column SNIPPED = Column.create(TABLE, "tv_snipped", Boolean.class);
        public static final Column SUB_HIGH_TIER = Column.create(TABLE, "tv_sub_high_tier", Boolean.class);
        public static final Column SUB_MID_TIER = Column.create(TABLE, "tv_sub_mid_tier", Boolean.class);

        public static final Column ARTWORK_URL = Column.create(TABLE, "tv_artwork_url", String.class);
        public static final Column IS_USER_LIKE = Column.create(TABLE, "tv_is_user_like", String.class);
        public static final Column IS_USER_REPOST = Column.create(TABLE, "tv_is_user_repost", String.class);

        public static final Column OFFLINE_REMOVED_AT = Column.create(TABLE, "tv_offline_removed_at", Long.class);
        public static final Column OFFLINE_DOWNLOADED_AT = Column.create(TABLE, "tv_offline_downloaded_at", Long.class);
        public static final Column OFFLINE_REQUESTED_AT = Column.create(TABLE, "tv_offline_requested_at", Long.class);
        public static final Column OFFLINE_UNAVAILABLE_AT = Column.create(TABLE, "tv_offline_unavailable_at", Long.class);

        static final String SQL = "CREATE VIEW IF NOT EXISTS TrackView AS " +
                Query.from(SoundView.name())
                     .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                             field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                             field(SoundView.field(TableColumns.SoundView.USERNAME)).as(CREATOR_NAME.name()),
                             field(SoundView.field(TableColumns.SoundView.USER_ID)).as(CREATOR_ID.name()),
                             field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.WAVEFORM_URL)).as(WAVEFORM_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.SNIPPET_DURATION)).as(SNIPPET_DURATION.name()),
                             field(SoundView.field(TableColumns.SoundView.FULL_DURATION)).as(FULL_DURATION.name()),

                             field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                             field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),

                             field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(PLAY_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.COMMENT_COUNT)).as(COMMENTS_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.COMMENTABLE)).as(IS_COMMENTABLE.name()),
                             field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),

                             field(SoundView.field(TableColumns.SoundView.POLICIES_POLICY)).as(POLICY.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZABLE)).as(MONETIZABLE.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZATION_MODEL)).as(
                                     MONETIZATION_MODEL.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_BLOCKED)).as(BLOCKED.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_SNIPPED)).as(SNIPPED.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_HIGH_TIER)).as(SUB_HIGH_TIER.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_MID_TIER)).as(SUB_MID_TIER.name()),

                             field(SoundView.field(TableColumns.SoundView.OFFLINE_DOWNLOADED_AT)).as(OFFLINE_DOWNLOADED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.OFFLINE_REMOVED_AT)).as(OFFLINE_REMOVED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.OFFLINE_REQUESTED_AT)).as(OFFLINE_REQUESTED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.OFFLINE_UNAVAILABLE_AT)).as(OFFLINE_UNAVAILABLE_AT.name()),

                             field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),

                             field(Tables.Likes._ID.qualifiedName() + " IS NOT NULL").as(IS_USER_LIKE.name()),
                             field(Tables.Posts.TYPE.qualifiedName() + " IS NOT NULL").as(IS_USER_REPOST.name()))

                     .leftJoin(Tables.Likes.TABLE, getLikeJoinConditions())
                     .leftJoin(Posts.TABLE, getRepostJoinConditions())
                     .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Sounds.TYPE_TRACK);

        private static Where getLikeJoinConditions() {
            return Filter.filter()
                         .whereEq(Table.SoundView.field(_ID), Tables.Likes._ID)
                         .whereEq(Table.SoundView.field(_TYPE), Tables.Likes._TYPE)
                         .whereNull(Tables.Likes.REMOVED_AT);
        }

        private static Where getRepostJoinConditions() {
            return Filter.filter()
                         .whereEq(Table.SoundView.field(_ID), Posts.TARGET_ID)
                         .whereEq(Table.SoundView.field(_TYPE), Posts.TARGET_TYPE)
                         .whereEq(Posts.TYPE.qualifiedName(), typeRepostDelimited());
        }

        private static String typeRepostDelimited() {
            return "'" + Posts.TYPE_REPOST + "'";
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecommendedPlaylistBucket extends SCBaseTable {
        public static final RecommendedPlaylistBucket TABLE = new RecommendedPlaylistBucket();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column KEY = Column.create(TABLE, "key", String.class);
        public static final Column DISPLAY_NAME = Column.create(TABLE, "display_name", String.class);
        public static final Column ARTWORK_URL = Column.create(TABLE, "artwork_url", String.class);
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn", String.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecommendedPlaylistBucket (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "key TEXT, " +
                "display_name TEXT, " +
                "artwork_url TEXT, " +
                "query_urn TEXT" +
                ");";

        protected RecommendedPlaylistBucket() {
            super("RecommendedPlaylistBucket", PrimaryKey.of("_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecommendedPlaylist extends SCBaseTable {
        public static final RecommendedPlaylist TABLE = new RecommendedPlaylist();

        public static final Column _ID = Column.create(TABLE, "_id", Long.class);
        public static final Column BUCKET_ID = Column.create(TABLE, "bucket_id", Long.class);
        public static final Column PLAYLIST_ID = Column.create(TABLE, "playlist_id", Long.class);

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecommendedPlaylist (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bucket_id INTEGER, " +
                "playlist_id INTEGER, " +
                "FOREIGN KEY(bucket_id) REFERENCES RecommendedPlaylistBucket(_id) " +
                ");";

        protected RecommendedPlaylist() {
            super("RecommendedPlaylist", PrimaryKey.of("_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }
}
