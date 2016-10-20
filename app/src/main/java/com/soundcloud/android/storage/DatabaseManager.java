package com.soundcloud.android.storage;

import static com.soundcloud.android.events.DatabaseMigrationEvent.forSuccessfulMigration;
import static com.soundcloud.android.storage.SchemaMigrationHelper.alterColumns;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropTable;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropView;
import static com.soundcloud.android.storage.SchemaMigrationHelper.recreate;
import static java.lang.String.format;

import com.soundcloud.android.events.DatabaseMigrationEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ErrorUtils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"}) // We know
public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 99;
    private static final String DATABASE_NAME = "SoundCloud";

    private static final AtomicReference<DatabaseMigrationEvent> migrationEvent = new AtomicReference<>();
    private static long migrationStart = 0L;

    private static DatabaseManager instance;
    private final ApplicationProperties applicationProperties;

    public static DatabaseManager getInstance(Context context, ApplicationProperties applicationProperties) {
        if (instance == null) {
            instance = new DatabaseManager(context, applicationProperties);
        }
        return instance;
    }

    // Do NOT use this constructor outside older tests. We need a single instance of this class going forward.
    @Deprecated
    public DatabaseManager(Context context, ApplicationProperties applicationProperties) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate(" + db + ")");

        try {
            for (SCBaseTable table : allTables()) {
                db.execSQL(table.getCreateSQL());
            }

            // legacy tables
            for (Table t : Table.values()) {
                if (!t.view) {
                    SchemaMigrationHelper.create(t, db);
                }
            }

            // views
            createViews(db);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRecreateDb(SQLiteDatabase db) {
        Log.d(TAG, "onRecreate(" + db + ")");

        for (SCBaseTable table : allTables()) {
            dropTable(table.name(), db);
        }

        dropTable("RecentStations", db);

        // legacy tables
        for (Table t : Table.values()) {
            if (!t.view) {
                SchemaMigrationHelper.drop(t, db);
            }
        }

        dropViews(db);
        onCreate(db);
    }

    private boolean recreateViews(SQLiteDatabase db) {
        try {
            dropViews(db);
            createViews(db);
            return true;
        } catch (SQLException exception) {
            handleMigrationException(exception, "Failed to recreate views");
            return false;
        }
    }

    private void dropViews(SQLiteDatabase db) {
        for (SCBaseTable view : allViews()) {
            dropView(view.name(), db);
        }

        // legacy tables
        for (Table t : Table.values()) {
            if (t.view) {
                SchemaMigrationHelper.drop(t, db);
            }
        }
    }

    private void createViews(SQLiteDatabase db) {
        for (Table t : Table.values()) {
            if (t.view) {
                SchemaMigrationHelper.create(t, db);
            }
        }

        for (SCBaseTable view : allViews()) {
            db.execSQL(view.getCreateSQL());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        migrationStart = System.currentTimeMillis();
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
                            // Previously added the Shortcuts table, which is no longer used
                            success = true;
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
                        case 70:
                            success = upgradeTo70(db, oldVersion);
                            break;
                        case 71:
                            success = upgradeTo71(db, oldVersion);
                            break;
                        case 72:
                            success = upgradeTo72(db, oldVersion);
                            break;
                        case 73:
                            success = upgradeTo73(db, oldVersion);
                            break;
                        case 74:
                            success = upgradeTo74(db, oldVersion);
                            break;
                        case 75:
                            success = upgradeTo75(db, oldVersion);
                            break;
                        case 76:
                            success = upgradeTo76(db, oldVersion);
                            break;
                        case 77:
                            success = upgradeTo77(db, oldVersion);
                            break;
                        case 78:
                            success = upgradeTo78(db, oldVersion);
                            break;
                        case 79:
                            success = upgradeTo79(db, oldVersion);
                            break;
                        case 80:
                            // We missed migration 80. Create an artificial migration 81
                            // to unlock people stuck on faulty 80
                            success = true;
                            break;
                        case 81:
                            success = upgradeTo81(db, oldVersion);
                            break;
                        case 82:
                            success = upgradeTo82(db, oldVersion);
                            break;
                        case 83:
                            success = upgradeTo83(db, oldVersion);
                            break;
                        case 84:
                            success = upgradeTo84(db, oldVersion);
                            break;
                        case 85:
                            success = upgradeTo85(db, oldVersion);
                            break;
                        case 86:
                            success = upgradeTo86(db, oldVersion);
                            break;
                        case 87:
                            success = upgradeTo87(db, oldVersion);
                            break;
                        case 88:
                            success = upgradeTo88(db, oldVersion);
                            break;
                        case 89:
                            success = upgradeTo89(db, oldVersion);
                            break;
                        case 90:
                            success = upgradeTo90(db, oldVersion);
                            break;
                        case 91:
                            success = upgradeTo91(db, oldVersion);
                            break;
                        case 92:
                            success = upgradeTo92(db, oldVersion);
                            break;
                        case 93:
                            success = upgradeTo93(db, oldVersion);
                            break;
                        case 94:
                            success = upgradeTo94(db, oldVersion);
                            break;
                        case 95:
                            success = upgradeTo95(db, oldVersion);
                            break;
                        case 96:
                            success = upgradeTo96(db, oldVersion);
                            break;
                        case 97:
                            success = upgradeTo97(db, oldVersion);
                            break;
                        case 98:
                            success = upgradeTo98(db, oldVersion);
                            break;
                        case 99:
                            success = upgradeTo99(db, oldVersion);
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
                Log.i(TAG, "successful db recreating views");
                success = recreateViews(db);
            }

            if (success) {
                Log.i(TAG, "successful db upgrade");
                migrationEvent.set(forSuccessfulMigration(getMigrationDuration()));
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

    public DatabaseMigrationEvent pullMigrationReport() {
        return migrationEvent.getAndSet(null);
    }

    /**
     * Added unavailable_at column to TrackDownloads table
     */
    private boolean upgradeTo36(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo37(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.PlaylistTracks, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 37);
        }
        return false;
    }

    /**
     * Added Posts table
     */
    private boolean upgradeTo38(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo39(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.OfflineContent.SQL);
            SchemaMigrationHelper.create(Table.TrackPolicies, db);
            migratePolicies(db);

            alterColumns(Table.Sounds, db);
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
    private boolean upgradeTo40(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo41(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo42(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo43(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.PromotedTracks, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 43);
        }
        return false;
    }

    /**
     * Add tier info to policies table
     */
    private boolean upgradeTo44(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.TrackPolicies, db);
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
    private boolean upgradeTo45(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo46(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo47(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.PlayQueue.TABLE.name(), Tables.PlayQueue.SQL, db);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 47);
        }
        return false;
    }

    /**
     * Recreate PlayQueue table to try to fix an unknown crash (that we think is migration related)
     */
    private boolean upgradeTo48(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo49(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo50(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Stations.TABLE.name(), db);
            dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.Stations.SQL);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 50);
        }
        return false;
    }

    /**
     * Add related entity to PlayQueue table (dropping and recreating because alter caused probs before)
     */
    private boolean upgradeTo51(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo52(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo53(SQLiteDatabase db, int oldVersion) {
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
     * Drop RecentStations
     */
    private boolean upgradeTo54(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable("RecentStations", db);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 54);
        }
        return false;
    }

    /**
     * Drop RecentStations
     */
    private boolean upgradeTo55(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable("RecentStations", db);
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
    private boolean upgradeTo56(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo57(SQLiteDatabase db, int oldVersion) {
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
     * Adds the URN column to the Comments table
     */
    private boolean upgradeTo59(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Comments.TABLE.name(), Tables.Comments.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 59);
        }
        return false;
    }

    /**
     * Adds the FULL_DURATION to the sounds table
     */
    private boolean upgradeTo60(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Sounds, db);
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
    private boolean upgradeTo61(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo62(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.TrackPolicies, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 62);
        }
        return false;
    }

    /**
     * Adds REMOVED_AT to the Sounds table
     */
    private boolean upgradeTo63(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Sounds, db);
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
    private boolean upgradeTo64(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.TrackPolicies, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 64);
        }
        return false;
    }

    /* Drop activities as some may be missing associated playlist entity (delete playlist bug) */
    private boolean upgradeTo65(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo66(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo67(SQLiteDatabase db, int oldVersion) {
        try {
            // this view isn't used anymore
            SchemaMigrationHelper.dropView("SoundAssociationView", db);

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
    private boolean upgradeTo68(SQLiteDatabase db, int oldVersion) {
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
    private boolean upgradeTo69(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.UserAssociations, db);
            SchemaMigrationHelper.recreate(Table.UserAssociationView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 69);
        }
        return false;
    }

    /*
     * Added snippet_duration to sounds
     */
    private boolean upgradeTo70(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Sounds, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 70);
        }
        return false;
    }

    /*
     * Made Uri the primary key of Collections
     */
    private boolean upgradeTo71(SQLiteDatabase db, int oldVersion) {
        try {
            recreate(Table.Collections, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 71);
        }
        return false;
    }

    /*
     * Added modified_at for playlist updates
     */
    private boolean upgradeTo72(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Sounds, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 72);
        }
        return false;
    }

    /*
     * Added artwork_url_template for stations
     */
    private boolean upgradeTo73(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Stations.TABLE.name(), Tables.Stations.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 73);
        }
        return false;
    }

    /*
     * Added updated_at for station play queues
     */
    private boolean upgradeTo74(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Stations.TABLE.name(), Tables.Stations.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 74);
        }
        return false;
    }

    /**
     * Added avatar_url to offline playlist tracks view for new image requirements
     * Create view for querying local search suggestions
     */
    private boolean upgradeTo75(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropView(Tables.OfflinePlaylistTracks.TABLE.name(), db);
            db.execSQL(Tables.OfflinePlaylistTracks.SQL);
            db.execSQL(Tables.SearchSuggestions.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 75);
        }
        return false;
    }

    /**
     * Created Charts table
     */
    private boolean upgradeTo76(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.Charts.SQL);
            db.execSQL(Tables.ChartTracks.SQL);
            return true;

        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 76);
        }
        return false;
    }

    /**
     * Creates local play history table
     */
    private boolean upgradeTo77(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.PlayHistory.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 77);
        }
        return false;
    }

    /**
     * Albums Support: Add is_album, set_type and release_date to Sounds
     */
    private boolean upgradeTo78(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Sounds, db);
            recreateSoundDependentViews(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 78);
        }
        return false;
    }

    /**
     * Edit charts table to match API changes
     */
    private boolean upgradeTo79(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Charts.TABLE.name(), db);
            dropTable(Tables.ChartTracks.TABLE.name(), db);
            db.execSQL(Tables.Charts.SQL);
            db.execSQL(Tables.ChartTracks.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 79);
        }
        return false;
    }

    /**
     * Artist stations in profiles, add artist_station to Users
     * Change Recommendations table structure for tracking.
     * Need query_urn and query_position for each recommended bucket.
     */
    private boolean upgradeTo81(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Users, db);
            dropTable(Tables.RecommendationSeeds.TABLE.name(), db);
            db.execSQL(Tables.RecommendationSeeds.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 81);
        }
        return false;
    }

    /**
     * Converge changes reflected in final schema but not through migrations
     * from database version 36 onwards
     */
    private boolean upgradeTo82(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable("CollectionItems", db);
            dropTable("CollectionPages", db);
            dropTable("Connections", db);
            dropTable("RecentStations", db);
            dropTable("Recordings", db);
            dropTable("Searches", db);
            dropTable("Suggestions", db);
            dropTable("TrackMetadata", db);
            alterColumns(Tables.Comments.TABLE.name(), Tables.Comments.SQL, db);
            SchemaMigrationHelper.recreate(Table.ActivityView, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 82);
        }
        return false;
    }

    /**
     * Add helper flag to ChartTracks
     */
    private boolean upgradeTo83(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Charts.TABLE.name(), db);
            dropTable(Tables.ChartTracks.TABLE.name(), db);
            db.execSQL(Tables.Charts.SQL);
            db.execSQL(Tables.ChartTracks.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 83);
        }
        return false;
    }


    /**
     * Change track_urn column to track_id in StationPlayQueues
     */
    private boolean upgradeTo84(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.StationsPlayQueues.TABLE.name(), db);
            db.execSQL(Tables.StationsPlayQueues.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 84);
        }
        return false;
    }

    /**
     * Change ChartTracks to store ImageResource instead of index to sounds
     */
    private boolean upgradeTo85(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.Charts.TABLE.name(), db);
            dropTable(Tables.ChartTracks.TABLE.name(), db);
            db.execSQL(Tables.Charts.SQL);
            db.execSQL(Tables.ChartTracks.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 85);
        }
        return false;
    }

    /**
     * Split PlayHistory (tracks+context) into PlayHistory (tracks)
     * and RecentlyPlayed (context)
     */
    private boolean upgradeTo86(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.RecentlyPlayed.SQL);
            tryMigratePlayHistory(db);
            alterColumns(Tables.PlayHistory.TABLE.name(), Tables.PlayHistory.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 86);
        }
        return false;
    }

    private boolean upgradeTo87(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.StationsCollections.TABLE.name(), Tables.StationsCollections.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 87);
        }
        return false;
    }

    /**
     * Remove Shortcuts table
     */
    private boolean upgradeTo88(SQLiteDatabase db, int oldVersion) {
        try {
            dropView("Shortcuts", db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 88);
        }
        return false;
    }

    /**
     * Create PlaylistView
     */
    private boolean upgradeTo89(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.PlaylistView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 89);
        }
        return false;
    }

    /**
     * Create UsersView
     */
    @SuppressWarnings("unused")
    private boolean upgradeTo90(SQLiteDatabase db, int oldVersion) {
        return true;
    }

    /**
     * Create TrackView
     * Create TrackView, recreate PlaylistView after changing column names
     */
    private boolean upgradeTo91(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.TrackView.SQL);

            SchemaMigrationHelper.dropView(Tables.PlaylistView.TABLE.name(), db);
            db.execSQL(Tables.PlaylistView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 91);
        }
        return false;
    }

    /**
     * Create Suggested Creators Table
     */
    private boolean upgradeTo92(SQLiteDatabase db, int oldVersion) {
        try {
            db.execSQL(Tables.SuggestedCreators.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 92);
        }
        return false;
    }

    /**
     * Add visual column to User table
     */
    private boolean upgradeTo93(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Table.Users, db);
            dropView(Tables.UsersView.TABLE.name(), db);
            db.execSQL(Tables.UsersView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 93);
        }
        return false;
    }

    /**
     * Add Context fields to PlayQueue table
     */
    private boolean upgradeTo94(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.PlayQueue.TABLE.name(), Tables.PlayQueue.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 94);
        }
        return false;
    }

    /**
     * New snipped column in OfflinePlaylistTracks view
     */
    private boolean upgradeTo95(SQLiteDatabase db, int oldVersion) {
        try {
            dropView(Tables.OfflinePlaylistTracks.TABLE.name(), db);
            db.execSQL(Tables.OfflinePlaylistTracks.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 95);
        }
        return false;
    }

    /**
     * Recreate PlaylistView after fixing local count column for efficiency
     */
    private boolean upgradeTo96(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropView(Tables.PlaylistView.TABLE.name(), db);
            db.execSQL(Tables.PlaylistView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 96);
        }
        return false;
    }

    /**
     * Recreate TrackView to try to fix column not found (that does exists) in some clients
     */
    private boolean upgradeTo97(SQLiteDatabase db, int oldVersion) {
        try {
            SchemaMigrationHelper.dropView(Tables.TrackView.TABLE.name(), db);
            db.execSQL(Tables.TrackView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 97);
        }
        return false;
    }

    /**
     * Add followed_at column to suggested creators
     */
    private boolean upgradeTo98(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.SuggestedCreators.TABLE.name(), Tables.SuggestedCreators.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 98);
        }
        return false;
    }

    /**
     * Add Genre + TagList to TrackView + PlaylistView
     */
    private boolean upgradeTo99(SQLiteDatabase db, int oldVersion) {
        try {
            dropView(Tables.TrackView.TABLE.name(), db);
            db.execSQL(Tables.TrackView.SQL);
            dropView(Tables.PlaylistView.TABLE.name(), db);
            db.execSQL(Tables.PlaylistView.SQL);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 99);
        }
        return false;
    }

    private void tryMigratePlayHistory(SQLiteDatabase db) {
        try {
            db.execSQL(Tables.RecentlyPlayed.MIGRATE_SQL);
        } catch (SQLException exception) {
            // ignore migration errors when already on new table schema
        }
    }

    private void migratePolicies(SQLiteDatabase db) {
        final List<String> oldSoundColumns = Arrays.asList(
                "_id", "monetizable", "policy");
        final List<String> newPoliciesColumns = Arrays.asList(
                TableColumns.TrackPolicies.TRACK_ID,
                TableColumns.TrackPolicies.MONETIZABLE,
                TableColumns.TrackPolicies.POLICY
        );

        SchemaMigrationHelper.migrate(db, Table.TrackPolicies.name(),
                                      newPoliciesColumns, Table.Sounds.name(), oldSoundColumns);
    }

    private void handleUpgradeException(SQLException exception, int oldVersion, int newVersion) {
        final String message = format(Locale.US, "error during upgrade%d (from %d)", newVersion, oldVersion);
        migrationEvent.set(DatabaseMigrationEvent.forFailedMigration(oldVersion,
                                                                     newVersion,
                                                                     getMigrationDuration(),
                                                                     exception.getMessage()));
        handleMigrationException(exception, message);
    }

    private void handleMigrationException(SQLException exception, String message) {

        if (applicationProperties.allowDatabaseMigrationsSilentErrors()) {
            ErrorUtils.handleSilentException(message, exception);
        } else {
            throw exception;
        }
    }

    private long getMigrationDuration() {
        return System.currentTimeMillis() - migrationStart;
    }

    private void recreateSoundDependentViews(SQLiteDatabase db) {
        SchemaMigrationHelper.recreate(Table.SoundView, db);
        SchemaMigrationHelper.recreate(Table.PlaylistTracksView, db);
        SchemaMigrationHelper.recreate(Table.SoundStreamView, db);
        SchemaMigrationHelper.recreate(Table.ActivityView, db);

        SchemaMigrationHelper.dropView(Tables.OfflinePlaylistTracks.TABLE.name(), db);
        db.execSQL(Tables.OfflinePlaylistTracks.SQL);

        SchemaMigrationHelper.dropView(Tables.PlaylistView.TABLE.name(), db);
        db.execSQL(Tables.PlaylistView.SQL);

        SchemaMigrationHelper.dropView(Tables.TrackView.TABLE.name(), db);
        db.execSQL(Tables.TrackView.SQL);
    }

    private List<SCBaseTable> allTables() {
        return Arrays.asList(
                Tables.Recommendations.TABLE,
                Tables.RecommendationSeeds.TABLE,
                Tables.PlayQueue.TABLE,
                Tables.Stations.TABLE,
                Tables.StationsPlayQueues.TABLE,
                Tables.StationsCollections.TABLE,
                Tables.TrackDownloads.TABLE,
                Tables.OfflineContent.TABLE,
                Tables.Comments.TABLE,
                Tables.Charts.TABLE,
                Tables.ChartTracks.TABLE,
                Tables.PlayHistory.TABLE,
                Tables.RecentlyPlayed.TABLE,
                Tables.SuggestedCreators.TABLE
        );
    }

    private List<SCBaseTable> allViews() {
        return Arrays.asList(
                Tables.SearchSuggestions.TABLE,
                Tables.OfflinePlaylistTracks.TABLE,
                Tables.PlaylistView.TABLE,
                Tables.UsersView.TABLE,
                Tables.TrackView.TABLE
        );
    }
}
