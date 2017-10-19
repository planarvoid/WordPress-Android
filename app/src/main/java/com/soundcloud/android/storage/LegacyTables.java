package com.soundcloud.android.storage;

import static android.provider.BaseColumns._ID;

import com.soundcloud.android.collection.playhistory.PlayHistoryStorage;
import com.soundcloud.propeller.schema.Column;

public final class LegacyTables {

    /**
     * @deprecated Play queue moved to its own storage {@link com.soundcloud.android.playback.PlayQueueStorage}
     */
    @Deprecated
    static class PlayQueue extends SCBaseTable {

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

    /**
     * @deprecated Play history moved to its own storage {@link PlayHistoryStorage}
     */
    @Deprecated
    static class PlayHistory extends SCBaseTable {
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

    /**
     * @deprecated RecentlyPlayed moved to its own storage {@link com.soundcloud.android.collection.DbModel.RecentlyPlayed}
     */
    @Deprecated
    static class RecentlyPlayed extends SCBaseTable {
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
}
