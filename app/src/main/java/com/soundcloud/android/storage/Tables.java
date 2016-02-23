package com.soundcloud.android.storage;

import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.propeller.schema.BaseTable;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

public interface Tables {

    class Recommendations extends BaseTable {

        // table instance
        public static final Recommendations TABLE = new Recommendations();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column SEED_ID = Column.create(TABLE, "seed_id");
        public static final Column RECOMMENDED_SOUND_ID = Column.create(TABLE, "recommended_sound_id");
        public static final Column RECOMMENDED_SOUND_TYPE = Column.create(TABLE, "recommended_sound_type");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Recommendations (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_id INTEGER, " +
                "recommended_sound_id INTEGER," +
                "recommended_sound_type INTEGER," +
                "FOREIGN KEY(seed_id) REFERENCES RecommendationSeeds(_id) " +
                "FOREIGN KEY(recommended_sound_id, recommended_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        protected Recommendations() {
            super("Recommendations", PrimaryKey.of(BaseColumns._ID));
        }
    }

    class RecommendationSeeds extends BaseTable {

        // table instance
        public static final RecommendationSeeds TABLE = new RecommendationSeeds();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column SEED_SOUND_ID = Column.create(TABLE, "seed_sound_id");
        public static final Column SEED_SOUND_TYPE = Column.create(TABLE, "seed_sound_type");
        public static final Column RECOMMENDATION_REASON = Column.create(TABLE, "recommendation_reason");

        public static final int REASON_LIKED = 0;
        public static final int REASON_LISTENED_TO = 1;

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecommendationSeeds (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_sound_id INTEGER, " +
                "seed_sound_type INTEGER, " +
                "recommendation_reason INTEGER, " +
                "FOREIGN KEY(seed_sound_id, seed_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        protected RecommendationSeeds() {
            super("RecommendationSeeds", PrimaryKey.of(BaseColumns._ID));
        }
    }

    class PlayQueue extends BaseTable {

        public static final PlayQueue TABLE = new PlayQueue();

        public static final Column ENTITY_ID = Column.create(TABLE, "entity_id");
        public static final Column ENTITY_TYPE = Column.create(TABLE, "entity_type");
        public static final Column REPOSTER_ID = Column.create(TABLE, "reposter_id");
        public static final Column RELATED_ENTITY = Column.create(TABLE, "related_entity");
        public static final Column SOURCE = Column.create(TABLE, "source");
        public static final Column SOURCE_VERSION = Column.create(TABLE, "source_version");
        public static final Column SOURCE_URN = Column.create(TABLE, "source_urn");
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn");

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
                "query_urn TEXT" +
                ");";

        protected PlayQueue() {
            super("PlayQueue", PrimaryKey.of(BaseColumns._ID));
        }
    }

    class Stations extends BaseTable {
        public static final Stations TABLE = new Stations();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column TYPE = Column.create(TABLE, "type");
        public static final Column TITLE = Column.create(TABLE, "title");
        public static final Column PERMALINK = Column.create(TABLE, "permalink");
        public static final Column LAST_PLAYED_TRACK_POSITION = Column.create(TABLE, "last_played_track_position");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Stations (" +
                "station_urn TEXT," +
                "type TEXT," +
                "title TEXT," +
                "permalink TEXT," +
                "last_played_track_position INTEGER DEFAULT NULL," +
                "PRIMARY KEY(station_urn) ON CONFLICT REPLACE" +
                ");";

        protected Stations() {
            super("Stations", PrimaryKey.of("station_urn"));
        }
    }

    class StationsPlayQueues extends BaseTable {
        public static final StationsPlayQueues TABLE = new StationsPlayQueues();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column TRACK_URN = Column.create(TABLE, "track_urn");
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn");
        public static final Column POSITION = Column.create(TABLE, "position");

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsPlayQueues (" +
                "station_urn TEXT," +
                "track_urn TEXT," +
                "query_urn TEXT," +
                "position INTEGER DEFAULT 0," +
                "PRIMARY KEY(station_urn, track_urn, position) ON CONFLICT REPLACE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        protected StationsPlayQueues() {
            super("StationsPlayQueues", PrimaryKey.of("station_urn", "track_urn", "position"));
        }
    }

    class StationsCollections extends BaseTable {
        public static final StationsCollections TABLE = new StationsCollections();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column COLLECTION_TYPE = Column.create(TABLE, "collection_type");
        public static final Column POSITION = Column.create(TABLE, "position");
        public static final Column UPDATED_LOCALLY_AT = Column.create(TABLE, "updated_locally_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsCollections (" +
                "station_urn TEXT NOT NULL," +
                "collection_type INTEGER NOT NULL," +
                "position INTEGER," +
                "updated_locally_at INTEGER," +
                "PRIMARY KEY(station_urn, collection_type) ON CONFLICT IGNORE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        protected StationsCollections() {
            super("StationsCollections", PrimaryKey.of("station_urn, collection_type"));
        }
    }

    class TrackDownloads extends BaseTable {

        public static final TrackDownloads TABLE = new TrackDownloads();

        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at");
        public static final Column REQUESTED_AT = Column.create(TABLE, "requested_at");
        public static final Column DOWNLOADED_AT = Column.create(TABLE, "downloaded_at");
        public static final Column UNAVAILABLE_AT = Column.create(TABLE, "unavailable_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS TrackDownloads (" +
                "_id INTEGER PRIMARY KEY," +
                "requested_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                "downloaded_at INTEGER DEFAULT NULL," +
                "removed_at INTEGER DEFAULT NULL," + // track marked for deletion
                "unavailable_at INTEGER DEFAULT NULL" +
                ");";

        protected TrackDownloads() {
            super("TrackDownloads", PrimaryKey.of(BaseColumns._ID));
        }
    }

    class OfflineContent extends BaseTable {

        public static final OfflineContent TABLE = new OfflineContent();

        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column _TYPE = Column.create(TABLE, "_type");

        public static final int TYPE_PLAYLIST = Sounds.TYPE_PLAYLIST;
        public static final int TYPE_COLLECTION = Sounds.TYPE_COLLECTION;

        public static final int ID_OFFLINE_LIKES = 0;

        static final String SQL = "CREATE TABLE IF NOT EXISTS OfflineContent (" +
                "_id INTEGER," +
                "_type INTEGER," +
                "PRIMARY KEY (_id, _type)," +
                "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
                ");";

        protected OfflineContent() {
            super("OfflineContent", PrimaryKey.of(BaseColumns._ID, "_type"));
        }
    }

    class Shortcuts extends BaseTable {

        public static final Shortcuts TABLE = new Shortcuts();

        public static final Column KIND = Column.create(TABLE, "kind");
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column _TYPE = Column.create(TABLE, "_type");
        public static final Column DISPLAY_TEXT = Column.create(TABLE, "display_text");

        public static final String TYPE_LIKE = "like";
        public static final String KIND_FOLLOWING = "following";

        static final String SQL = "CREATE VIEW IF NOT EXISTS Shortcuts AS SELECT 'like' AS kind, " +
                "Sounds._id AS _id, " +
                "Sounds._type AS _type, " +
                "title AS display_text " +
                "FROM Likes INNER JOIN Sounds ON Likes._id = Sounds._id " +
                "AND Likes._type = Sounds._type" +
                " UNION" +
                " SELECT 'following' AS kind, " +
                "Users._id, 0 AS _type, " +
                "username AS text " +
                "from UserAssociations INNER JOIN Users ON UserAssociations.target_id = Users._id";

        protected Shortcuts() {
            super("Shortcuts", PrimaryKey.of(BaseColumns._ID, "kind"));
        }
    }

    class Comments extends BaseTable {

        public static final Comments TABLE = new Comments();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID);
        public static final Column URN = Column.create(TABLE, "urn");
        public static final Column USER_ID = Column.create(TABLE, "user_id");
        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp");
        public static final Column CREATED_AT = Column.create(TABLE, "created_at");
        public static final Column BODY = Column.create(TABLE, "body");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Comments (" +
                "_id INTEGER PRIMARY KEY," +
                "urn TEXT UNIQUE," +
                "user_id INTEGER," +
                "track_id INTEGER," +
                "timestamp INTEGER," +
                "created_at INTEGER," +
                "body TEXT" +
                ");";

        Comments() {
            super("Comments", PrimaryKey.of(BaseColumns._ID));
        }
    }

    class OfflinePlaylistTracks extends BaseTable {

        public static final OfflinePlaylistTracks TABLE = new OfflinePlaylistTracks();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID);
        public static final Column _TYPE = Column.create(TABLE, "_type");
        public static final Column USER_ID = Column.create(TABLE, "user_id");
        public static final Column DURATION = Column.create(TABLE, "duration");
        public static final Column WAVEFORM_URL = Column.create(TABLE, "waveform_url");
        public static final Column SYNCABLE = Column.create(TABLE, "syncable");
        public static final Column LAST_POLICY_UPDATE = Column.create(TABLE, "last_policy_update");
        public static final Column CREATED_AT = Column.create(TABLE, "created_at");
        public static final Column POSITION = Column.create(TABLE, "position");

        static final String SQL = "CREATE VIEW IF NOT EXISTS OfflinePlaylistTracks AS " +
                "SELECT " +
                "Sounds._id as _id," +
                "Sounds._type as _type, " +
                "Sounds.user_id as user_id, " +
                "Sounds.duration as duration, " +
                "Sounds.waveform_url as waveform_url, " +
                "TrackPolicies.syncable as syncable, " +
                "TrackPolicies.last_updated as last_policy_update, " +
                "PlaylistTracks.position as position, " +
                "MAX(IFNULL(PlaylistLikes.created_at, 0), PlaylistProperties.created_at ) AS created_at " +
                // ^ The timestamp used to sort
                "FROM Sounds " +
                    "INNER JOIN PlaylistTracks ON Sounds._id = PlaylistTracks.track_id " +
                    // ^ Add PlaylistTracks to tracks
                    "LEFT JOIN Likes as PlaylistLikes ON (PlaylistTracks.playlist_id = PlaylistLikes._id) AND (PlaylistLikes._type = " + TableColumns.Sounds.TYPE_PLAYLIST + ") " +
                    // ^ When available, adds the Playlist Like date to the tracks (for sorting purpose)
                    "LEFT JOIN Sounds as PlaylistProperties ON (PlaylistProperties._id = PlaylistTracks.playlist_id AND PlaylistProperties._type = " + TableColumns.Sounds.TYPE_PLAYLIST + ")" +
                    // ^ Add the playlist creation date
                    "INNER JOIN OfflineContent ON PlaylistTracks.playlist_id = OfflineContent._id  AND Sounds._type = " + TableColumns.Sounds.TYPE_TRACK + " " +
                    // ^ Keep only offline tracks
                    "INNER JOIN TrackPolicies ON PlaylistTracks.track_id = TrackPolicies.track_id " +
                    // ^ Keep only tracks with policies
                "WHERE (PlaylistTracks.removed_at IS NULL) ";

        OfflinePlaylistTracks() {
            super("OfflinePlaylistTracks", PrimaryKey.of(BaseColumns._ID));
        }

    }
}
