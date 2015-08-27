package com.soundcloud.android.storage;

import com.soundcloud.propeller.schema.BaseTable;

interface LegacyTables {
    // This has been replaced by StationsCollections
    // Left in place for migration purposes.
    @Deprecated
    class RecentStations extends BaseTable {
        public static final RecentStations TABLE = new RecentStations();

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecentStations (" +
                "station_urn TEXT," +
                "position INTEGER NOT NULL," +
                "PRIMARY KEY(station_urn) ON CONFLICT REPLACE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        protected RecentStations() {
            super("RecentStations", PrimaryKey.of("station_urn"));
        }
    }
}
