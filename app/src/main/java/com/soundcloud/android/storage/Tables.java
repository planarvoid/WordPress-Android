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

        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column REPOSTER_ID = Column.create(TABLE, "reposter_id");
        public static final Column RELATED_ENTITY = Column.create(TABLE, "related_entity");
        public static final Column SOURCE = Column.create(TABLE, "source");
        public static final Column SOURCE_VERSION = Column.create(TABLE, "source_version");

        static final String SQL =  "CREATE TABLE IF NOT EXISTS PlayQueue (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "track_id INTEGER," +
                "reposter_id INTEGER," +
                "related_entity TEXT," +
                "source VARCHAR(255)," +
                "source_version VARCHAR(255)" +
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
                "last_played_track_position INTEGER DEFAULT 0," +
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
        public static final Column POSITION = Column.create(TABLE, "position");

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsPlayQueues (" +
                "station_urn TEXT," +
                "track_urn TEXT," +
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
}
