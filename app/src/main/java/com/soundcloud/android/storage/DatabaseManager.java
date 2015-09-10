package com.soundcloud.android.storage;

import static com.soundcloud.android.storage.SchemaMigrationHelper.dropTable;

import com.soundcloud.android.utils.ErrorUtils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"}) // We know
public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 54;
    private static final String DATABASE_NAME = "SoundCloud";

    private static DatabaseManager instance;

    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    // Do NOT use this constructor outside older tests. We need a single instance of this class going forward.
    @Deprecated
    public DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate(" + db + ")");

        try {
            // new tables ! remember to drop in onRecreate!
            db.execSQL(Tables.Recommendations.SQL);
            db.execSQL(Tables.RecommendationSeeds.SQL);
            db.execSQL(Tables.PlayQueue.SQL);
            db.execSQL(Tables.Stations.SQL);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            db.execSQL(Tables.RecentStations.SQL);
            db.execSQL(Tables.TrackDownloads.SQL);
            db.execSQL(Tables.OfflineContent.SQL);

            // legacy tables
            for (Table t : Table.values()) {
                SchemaMigrationHelper.create(t, db);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void onRecreateDb(SQLiteDatabase db) {
        Log.d(TAG, "onRecreate(" + db + ")");

        dropTable(Tables.Recommendations.TABLE.name(), db);
        dropTable(Tables.RecommendationSeeds.TABLE.name(), db);
        dropTable(Tables.PlayQueue.TABLE.name(), db);
        dropTable(Tables.Stations.TABLE.name(), db);
        dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
        dropTable(Tables.RecentStations.TABLE.name(), db);
        dropTable(Tables.TrackDownloads.TABLE.name(), db);
        dropTable(Tables.OfflineContent.TABLE.name(), db);

        // legacy tables
        for (Table t : Table.values()) {
            SchemaMigrationHelper.drop(t, db);
        }
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (newVersion > oldVersion) {
            db.beginTransaction();
            boolean success = false;
            if (oldVersion >= 35) {
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 36:
                            success = upgradeTo36(db, oldVersion);
                            break;
                        case 37:
                            success = upgradeTo37(db, oldVersion);
                            break;
                        case 38:
                            success = upgradeTo38(db, oldVersion);
                            break;
                        case 39:
                            success = upgradeTo39(db, oldVersion);
                            break;
                        case 40:
                            success = upgradeTo40(db, oldVersion);
                            break;
                        case 41:
                            success = upgradeTo41(db, oldVersion);
                            break;
                        case 42:
                            success = upgradeTo42(db, oldVersion);
                            break;
                        case 43:
                            success = upgradeTo43(db, oldVersion);
                            break;
                        case 44:
                            success = upgradeTo44(db, oldVersion);
                            break;
                        case 45:
                            success = upgradeTo45(db, oldVersion);
                            break;
                        case 46:
                            success = upgradeTo46(db, oldVersion);
                            break;
                        case 47:
                            success = upgradeTo47(db, oldVersion);
                            break;
                        case 48:
                            success = upgradeTo48(db, oldVersion);
                            break;
                        case 49:
                            success = upgradeTo49(db, oldVersion);
                            break;
                        case 50:
                            success = upgradeTo50(db, oldVersion);
                            break;
                        case 51:
                            success = upgradeTo51(db, oldVersion);
                            break;
                        case 52:
                            success = upgradeTo52(db, oldVersion);
                            break;
                        case 53:
                            success = upgradeTo53(db, oldVersion);
                            break;
                        case 54:
                            success = upgradeTo54(db, oldVersion);
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
                onRecreateDb(db);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        } else {
            onRecreateDb(db);
        }
    }

    /**
     * Added unavailable_at column to TrackDownloads table
     */
    private static boolean upgradeTo36(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.TrackDownloads.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 36);
        }
        return false;
    }

    /**
     * Added removed_at column to PlaylistTracks
     */
    private static boolean upgradeTo37(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.PlaylistTracks, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 37);
        }
        return false;
    }

    /**
     * Added Posts table
     */
    private static boolean upgradeTo38(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.create(Table.Posts, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 38);
        }
        return false;
    }

    /**
     * Added OfflineContent and TrackPolicies tables
     */
    private static boolean upgradeTo39(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.OfflineContent.SQL);
            SchemaMigrationHelper.create(Table.TrackPolicies, db);
            migratePolicies(db);

            SchemaMigrationHelper.alterColumns(Table.Sounds, db);
            SchemaMigrationHelper.recreate(Table.SoundView, db);
            SchemaMigrationHelper.recreate(Table.SoundAssociationView, db);
            SchemaMigrationHelper.recreate(Table.PlaylistTracksView, db);
            SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
            SchemaMigrationHelper.recreate(Table.ActivityView, db);

            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 39);
        }
        return false;
    }

    /**
     * Changed Post table column names
     */
    private static boolean upgradeTo40(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.recreate(Table.Posts, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 40);
        }
        return false;
    }

    /**
     * Changed columns in PromotedTracks and SoundStreamView
     */
    private static boolean upgradeTo41(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.recreate(Table.PromotedTracks, db);
            SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 41);
        }
        return false;
    }

    /**
     * Update stream deduplication logic
     */
    private static boolean upgradeTo42(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 42);
        }
        return false;
    }

    /**
     * Add timestamp to promoted tracks
     */
    private static boolean upgradeTo43(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.PromotedTracks, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 43);
        }
        return false;
    }

    /**
     * Add tier info to policies table
     */
    private static boolean upgradeTo44(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.TrackPolicies, db);
            SchemaMigrationHelper.recreate(Table.SoundView, db);
            SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 44);
        }
        return false;
    }

    /**
     * Add waveforms table
     */
    private static boolean upgradeTo45(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.create(Table.Waveforms, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 45);
        }
        return false;
    }

    /**
     * Created Recommendations table
     */
    private static boolean upgradeTo46(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.Recommendations.SQL);
            db.execSQL(Tables.RecommendationSeeds.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 46);
        }
        return false;
    }

    /**
     * Add columns to PlayQueue table
     */
    private static boolean upgradeTo47(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Tables.PlayQueue.TABLE.name(), Tables.PlayQueue.SQL, db);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 47);
        }
        return false;
    }

    /**
     * Recreate PlayQueue table to try to fix an unknown crash (that we think is migration related)
     */
    private static boolean upgradeTo48(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.PlayQueue.TABLE.name(), db);
            db.execSQL(Tables.PlayQueue.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 48);
        }
        return false;
    }

    /**
     * Created Stations table
     */
    private static boolean upgradeTo49(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.Stations.SQL);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 49);
        }
        return false;
    }

    /**
     * Migrate Stations table to use the station_urn as the primary key & created Recently Played Stations table
     */
    private static boolean upgradeTo50(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Stations.TABLE.name(), db);
            dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.Stations.SQL);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            db.execSQL(Tables.RecentStations.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 50);
        }
        return false;
    }

    /**
     * Add related entity to PlayQueue table (dropping and recreating because alter caused probs before)
     */
    private static boolean upgradeTo51(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.PlayQueue.TABLE.name(), db);
            db.execSQL(Tables.PlayQueue.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 51);
        }
        return false;
    }

    /**
     * Update the primary key for the StationsPlayQueues
     */
    private static boolean upgradeTo52(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 52);
        }
        return false;
    }

    /**
     * Remove a column (seed) from the Stations table.
     */
    private static boolean upgradeTo53(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Stations.TABLE.name(), db);
            db.execSQL(Tables.Stations.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 53);
        }
        return false;
    }

    /**
     * Rename STARTED_AT Column to UPDATED_LOCALLY_AT and add the POSITION column
     * to the Recent Stations Table
     */
    private static boolean upgradeTo54(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.RecentStations.TABLE.name(), db);
            db.execSQL(Tables.RecentStations.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 54);
        }
        return false;
    }

    private static void migratePolicies(SQLiteDatabase db) {
        final List<String> oldSoundColumns = Arrays.asList(
                "_id", "monetizable", "policy");
        final List<String> newPoliciesColumns = Arrays.asList(
                TableColumns.TrackPolicies.TRACK_ID,
                TableColumns.TrackPolicies.MONETIZABLE,
                TableColumns.TrackPolicies.POLICY
        );

        SchemaMigrationHelper.migrate(db, Table.TrackPolicies.name(), newPoliciesColumns, Table.Sounds.name(), oldSoundColumns);
    }

    private static void handleUpgradeException(SQLException exception, int oldVersion, int newVersion) {
        final String message =
                String.format(Locale.US, "error during upgrade%d (from %d)", newVersion, oldVersion);
        ErrorUtils.handleSilentException(message, exception);
    }
}
