package com.soundcloud.android.storage;

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
        public static final Column SOURCE = Column.create(TABLE, "source");
        public static final Column SOURCE_VERSION = Column.create(TABLE, "source_version");

        static final String SQL =  "CREATE TABLE IF NOT EXISTS PlayQueue (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "track_id INTEGER," +
                "reposter_id INTEGER," +
                "source VARCHAR(255)," +
                "source_version VARCHAR(255)" +
                ");";

        protected PlayQueue() {
            super("PlayQueue", PrimaryKey.of(BaseColumns._ID));
        }
    }
}
