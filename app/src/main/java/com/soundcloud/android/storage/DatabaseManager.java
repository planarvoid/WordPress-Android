package com.soundcloud.android.storage;

import static com.soundcloud.android.storage.SchemaMigrationHelper.alterColumns;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropTable;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropView;

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
    public static final int DATABASE_VERSION = 69;
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
            db.execSQL(LegacyTables.RecentStations.SQL);
            db.execSQL(Tables.TrackDownloads.SQL);
            db.execSQL(Tables.OfflineContent.SQL);
            db.execSQL(Tables.StationsCollections.SQL);

            // legacy tables
            for (Table t : Table.values()) {
                SchemaMigrationHelper.create(t, db);
            }

            // views
            db.execSQL(Tables.Shortcuts.SQL);
            db.execSQL(Tables.OfflinePlaylistTracks.SQL);

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
        dropTable(Tables.StationsCollections.TABLE.name(), db);
        dropTable(Tables.TrackDownloads.TABLE.name(), db);
        dropTable(Tables.OfflineContent.TABLE.name(), db);
        dropTable(LegacyTables.RecentStations.TABLE.name(), db);
        dropView(Tables.OfflinePlaylistTracks.TABLE.name(), db);

        // legacy tables
        for (Table t : Table.values()) {
            SchemaMigrationHelper.drop(t, db);
        }
        dropView(Tables.Shortcuts.TABLE.name(), db);

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
                        case 55:
                            success = upgradeTo55(db, oldVersion);
                            break;
                        case 56:
                            success = upgradeTo56(db, oldVersion);
                            break;
                        case 57:
                            success = upgradeTo57(db, oldVersion);
                            break;
                        case 58:
                            success = upgradeTo58(db, oldVersion);
                            break;
                        case 59:
                            success = upgradeTo59(db, oldVersion);
                            break;
                        case 60:
                            success = upgradeTo60(db, oldVersion);
                            break;
                        case 61:
                            success = upgradeTo61(db, oldVersion);
                            break;
                        case 62:
                            success = upgradeTo62(db, oldVersion);
                            break;
                        case 63:
                            success = upgradeTo63(db, oldVersion);
                            break;
                        case 64:
                            success = upgradeTo64(db, oldVersion);
                            break;
                        case 65:
                            success = upgradeTo65(db, oldVersion);
                            break;
                        case 66:
                            success = upgradeTo66(db, oldVersion);
                            break;
                        case 67:
                            success = upgradeTo67(db, oldVersion);
                            break;
                        case 68:
                            success = upgradeTo68(db, oldVersion);
                            break;
                        case 69:
                            success = upgradeTo69(db, oldVersion);
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
            recreateSoundDependentViews(db);

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
            db.execSQL(LegacyTables.RecentStations.SQL);
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
            dropTable(LegacyTables.RecentStations.TABLE.name(), db);
            db.execSQL(LegacyTables.RecentStations.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 54);
        }
        return false;
    }

    /**
     * Rename RecentStations to StationsCollections and add the COLLECTION_TYPE column
     */
    private static boolean upgradeTo55(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropTable(LegacyTables.RecentStations.TABLE.name(), db);
            db.execSQL(Tables.StationsCollections.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 55);
        }
        return false;
    }

    /**
     * Update StationsPlayQueues to allow duplicated tracks.
     */
    private static boolean upgradeTo56(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 56);
        }
        return false;
    }

    /**
     * Update Stations table to start with correct default value for LAST_PLAYED_TRACK_POSITION
     */
    private static boolean upgradeTo57(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropTable(Tables.Stations.TABLE.name(), db);
            db.execSQL(Tables.Stations.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 57);
        }
        return false;
    }

    /**
     * Create Shortcuts view for querying ShortCuts
     */
    private static boolean upgradeTo58(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.Shortcuts.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 58);
        }
        return false;
    }

    /**
     * Adds the URN column to the Comments table
     */
    private static boolean upgradeTo59(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Tables.Comments.TABLE.name(), Tables.Comments.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 59);
        }
        return false;
    }

    /**
     * Adds the FULL_DURATION to the sounds table
     */
    private static boolean upgradeTo60(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.Sounds, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 60);
        }
        return false;
    }

    /**
     * Adds QUERY_URN & SOURCE_URN to the PlayQueue table
     * Adds the QUERY_URN column to the StationsPlayQueues table
     */
    private static boolean upgradeTo61(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.PlayQueue.TABLE.name(), db);
            dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.PlayQueue.SQL);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 61);
        }
        return false;
    }

    /*
     * Adds the BLOCKED to the track policy table
     */
    private static boolean upgradeTo62(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.TrackPolicies, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 62);
        }
        return false;
    }

    /**
     * Adds REMOVED_AT to the Sounds table
     */
    private static boolean upgradeTo63(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.Sounds, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 63);
        }
        return false;
    }


    /*
     * Adds the SNIPPED to the track policy table
     */
    private static boolean upgradeTo64(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.alterColumns(Table.TrackPolicies, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 64);
        }
        return false;
    }

    /* Drop activities as some may be missing associated playlist entity (delete playlist bug) */
    private static boolean upgradeTo65(SQLiteDatabase db, int oldVersion) {
        try {
            db.delete(Table.Activities.name(), null, null);
            SchemaMigrationHelper.recreate(Table.ActivityView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 65);
        }
        return false;
    }

    /*
     * Changes columns of PlayQueue table to handle playlists
     */
    private static boolean upgradeTo66(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.PlayQueue.TABLE.name(), db);
            db.execSQL(Tables.PlayQueue.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 66);
        }
        return false;
    }

    /*
     * Recreates SoundView to exclude tracks that don't have a policy entry
     */
    private static boolean upgradeTo67(SQLiteDatabase db, int oldVersion) {
        try {
            // this view isn't used anymore
            SchemaMigrationHelper.dropTable("SoundAssociationView", db);

            SchemaMigrationHelper.recreate(Table.SoundView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 67);
        }
        return false;
    }

    /*
     * Fix SoundAssociationView dropping
     * Creates view OfflinePlaylistTracks
     */
    private static boolean upgradeTo68(SQLiteDatabase db, int oldVersion) {
        try {
            // this view isn't used anymore
            dropView("SoundAssociationView", db);
            db.execSQL(Tables.OfflinePlaylistTracks.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 68);
        }
        return false;
    }

    /*
     * Remove Owner from User associations
     */
    private static boolean upgradeTo69(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.UserAssociations, db);
            SchemaMigrationHelper.recreate(Table.UserAssociationView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 69);
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

    private static void recreateSoundDependentViews(SQLiteDatabase db) {
        SchemaMigrationHelper.recreate(Table.SoundView, db);
        SchemaMigrationHelper.recreate(Table.PlaylistTracksView, db);
        SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
        SchemaMigrationHelper.recreate(Table.ActivityView, db);

        SchemaMigrationHelper.dropView(Tables.OfflinePlaylistTracks.TABLE.name(), db);
        db.execSQL(Tables.OfflinePlaylistTracks.SQL);
    }
}
