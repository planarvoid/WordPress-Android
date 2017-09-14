package com.soundcloud.android.storage;

import static com.soundcloud.android.events.DatabaseMigrationEvent.forSuccessfulMigration;
import static com.soundcloud.android.storage.SchemaMigrationHelper.alterColumns;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropTable;
import static com.soundcloud.android.storage.SchemaMigrationHelper.dropView;
import static com.soundcloud.android.storage.SchemaMigrationHelper.recreate;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.soundcloud.android.events.DatabaseMigrationEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"}) // We know
public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 119;
    private static final String DATABASE_NAME = "SoundCloud";

    private static final AtomicReference<DatabaseMigrationEvent> migrationEvent = new AtomicReference<>();
    private static long migrationStart = 0L;

    private static DatabaseManager instance;
    private final Context context;
    private final ApplicationProperties applicationProperties;

    public static DatabaseManager getInstance(Context context, ApplicationProperties applicationProperties) {
        if (instance == null) {
            instance = new DatabaseManager(context, applicationProperties);
        }
        return instance;
    }

    /**
     * @deprecated Do NOT use this constructor outside older tests. We need a single instance of this class going forward.
     */
    @Deprecated
    public DatabaseManager(Context context, ApplicationProperties applicationProperties) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
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
            for (Table table : allLegacyTables()) {
                SchemaMigrationHelper.create(table, db);
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
        for (Table table : allLegacyTables()) {
            SchemaMigrationHelper.drop(table, db);
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

        // legacy views
        for (Table view : allLegacyViews()) {
            SchemaMigrationHelper.drop(view, db);
        }
    }

    private void createViews(SQLiteDatabase db) {
        // legacy views
        for (Table view : allLegacyViews()) {
            SchemaMigrationHelper.create(view, db);
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
                        case 43:
                            success = upgradeTo43(db, oldVersion);
                            break;
                        case 44:
                            success = upgradeTo44(db, oldVersion);
                            break;
                        case 45:
                            success = upgradeTo45();
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
                        case 77:
                            success = upgradeTo77(db, oldVersion);
                            break;
                        case 78:
                            success = upgradeTo78(db, oldVersion);
                            break;
                        case 81:
                            success = upgradeTo81(db, oldVersion);
                            break;
                        case 82:
                            success = upgradeTo82(db, oldVersion);
                            break;
                        case 84:
                            success = upgradeTo84(db, oldVersion);
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
                        case 92:
                            success = upgradeTo92(db, oldVersion);
                            break;
                        case 93:
                            success = upgradeTo93(db, oldVersion);
                            break;
                        case 94:
                            success = upgradeTo94(db, oldVersion);
                            break;
                        case 98:
                            success = upgradeTo98(db, oldVersion);
                            break;
                        case 103:
                            success = upgradeTo103(db, oldVersion);
                            break;
                        case 104:
                            success = upgradeTo104(db, oldVersion);
                            break;
                        case 108:
                            success = upgradeTo108(db, oldVersion);
                            break;
                        case 110:
                            success = upgradeTo110(db, oldVersion);
                            break;
                        case 112:
                            success = upgradeTo112(db, oldVersion);
                            break;
                        case 113:
                            success = upgradeTo113(db, oldVersion);
                            break;
                        case 114:
                            success = upgradeTo114(db, oldVersion);
                            break;
                        case 115:
                            success = upgradeTo115(db, oldVersion);
                            break;
                        case 118:
                            success = upgradeTo118(db, oldVersion);
                            break;
                        case 119:
                            success = upgradeTo119(oldVersion);
                            break;
                        case 120:
                            success = upgradeTo120(db, oldVersion);
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

    public void clearTables() {
        SQLiteDatabase db = getWritableDatabase();

        for (SCBaseTable table : allTables()) {
            clearTable(table.name(), db);
        }

        // legacy tables
        for (Table table : allLegacyTables()) {
            clearTable(table.name(), db);
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
            db.execSQL(Tables.Posts.TABLE.getCreateSQL());
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
            db.execSQL(Tables.OfflineContent.TABLE.getCreateSQL());
            db.execSQL(Tables.TrackPolicies.TABLE.getCreateSQL());
            migratePolicies(db);

            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
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
            SchemaMigrationHelper.recreateTable(Tables.Posts.TABLE, db);
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
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 41);
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
            alterColumns(Tables.TrackPolicies.TABLE.name(), Tables.TrackPolicies.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 44);
        }
        return false;
    }

    /**
     * Add waveforms table
     */
    private boolean upgradeTo45() {
        // removed when we migrated waveforms out of db
        return true;
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
            SchemaMigrationHelper.recreateTable(Tables.PlayQueue.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.Stations.TABLE, db);
            SchemaMigrationHelper.recreateTable(Tables.StationsPlayQueues.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.PlayQueue.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.StationsPlayQueues.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.Stations.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.StationsPlayQueues.TABLE, db);
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
            SchemaMigrationHelper.recreateTable(Tables.Stations.TABLE, db);
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
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
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
            alterColumns(Tables.TrackPolicies.TABLE.name(), Tables.TrackPolicies.SQL, db);
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
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
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
            alterColumns(Tables.TrackPolicies.TABLE.name(), Tables.TrackPolicies.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 64);
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
     * Drop SoundAssociationView view
     */
    private boolean upgradeTo67(SQLiteDatabase db, int oldVersion) {
        try {
            // this view isn't used anymore
            SchemaMigrationHelper.dropView("SoundAssociationView", db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 67);
        }
        return false;
    }

    /*
     * Fix SoundAssociationView dropping
     */
    private boolean upgradeTo68(SQLiteDatabase db, int oldVersion) {
        try {
            // this view isn't used anymore
            dropView("SoundAssociationView", db);
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
            SchemaMigrationHelper.recreateTable(Tables.UserAssociations.TABLE, db);
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
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
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
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
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
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
            // view recreation is done at the end of the migration
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 78);
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
            alterColumns(Tables.Users.TABLE.name(), Tables.Users.SQL, db);
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
            // view recreation is done at the end of the migration
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 82);
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
            alterColumns(Tables.Users.TABLE.name(), Tables.Users.SQL, db);
            // view recreation is done at the end of the migration
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
     * Drop no longer used UserAssociationView
     */
    private boolean upgradeTo103(SQLiteDatabase db, int oldVersion) {
        try {
            dropView("UserAssociationView", db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 103);
        }
        return false;
    }

    /**
     * Add played column to PlayQueue table
     */
    private boolean upgradeTo104(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.PlayQueue.TABLE.name(), Tables.PlayQueue.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 104);
        }
        return false;
    }

    /**
     * Change the "Users" table to contain firstname, lastname, signup_date
     */
    private boolean upgradeTo108(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Users.TABLE.name(), Tables.Users.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 108);
        }
        return false;
    }

    /**
     * Remove SearchSuggestions view.
     */
    private boolean upgradeTo110(SQLiteDatabase db, int oldVersion) {
        try {
            dropView("SearchSuggestions", db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 110);
        }
        return false;
    }

    /**
     * Waveforms moved to separate storage class
     */
    private boolean upgradeTo112(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable("Waveforms", db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 112);
        }
        return false;
    }

    private boolean upgradeTo113(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.PlayHistory.TABLE.name(), db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 113);
        }
        return false;
    }

    private boolean upgradeTo114(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable(Tables.RecentlyPlayed.TABLE.name(), db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 114);
        }
        return false;
    }

    /**
     * Add isPro column to User and UserView
     */
    private boolean upgradeTo115(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Users.TABLE.name(), Tables.Users.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 115);
        }
        return false;
    }

    private boolean upgradeTo118(SQLiteDatabase db, int oldVersion) {
        try {
            alterColumns(Tables.Sounds.TABLE.name(), Tables.Sounds.SQL, db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 118);
        }
        return false;
    }

    /**
     * Added SYNCABLE and POLICY_LAST_UPDATED_AT column to sound view
     */
    private boolean upgradeTo119(int oldVersion) {
        // sound view changed
        return true;
    }

    private boolean upgradeTo120(SQLiteDatabase db, int oldVersion) {
        try {
            dropTable("Recommendations", db);
            dropTable("RecommendationSeeds", db);
            dropTable("RecommendedPlaylistBucket", db);
            dropTable("RecommendedPlaylist", db);
            dropTable("Charts", db);
            dropTable("ChartTracks", db);
            context.deleteFile("storage_newforyou");
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 119);
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
        final List<String> oldSoundColumns = asList(
                "_id", "monetizable", "policy");
        final List<String> newPoliciesColumns = asList(
                Tables.TrackPolicies.TRACK_ID.name(),
                Tables.TrackPolicies.MONETIZABLE.name(),
                Tables.TrackPolicies.POLICY.name()
        );

        SchemaMigrationHelper.migrate(db, Tables.TrackPolicies.TABLE.name(),
                                      newPoliciesColumns, Tables.Sounds.TABLE.name(), oldSoundColumns);
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

    private void clearTable(String tableName, SQLiteDatabase db) {
        Log.d(TAG, "clearing " + tableName);
        db.execSQL("DELETE FROM " + tableName);
    }

    private List<SCBaseTable> allTables() {
        return asList(
                Tables.Users.TABLE,
                Tables.Sounds.TABLE,
                Tables.TrackPolicies.TABLE,
                Tables.Likes.TABLE,
                Tables.Posts.TABLE,
                Tables.UserAssociations.TABLE,
                Tables.PlayQueue.TABLE,
                Tables.Stations.TABLE,
                Tables.StationsPlayQueues.TABLE,
                Tables.StationsCollections.TABLE,
                Tables.TrackDownloads.TABLE,
                Tables.OfflineContent.TABLE,
                Tables.Comments.TABLE,
                Tables.SuggestedCreators.TABLE
        );
    }

    private List<SCBaseTable> allViews() {
        return asList(
                Tables.OfflinePlaylistTracks.TABLE,
                Tables.PlaylistView.TABLE,
                Tables.UsersView.TABLE,
                Tables.TrackView.TABLE
        );
    }

    private List<Table> allLegacyTables() {
        return newArrayList(filter(asList(Table.values()), t -> !t.view));
    }

    private List<Table> allLegacyViews() {
        return newArrayList(filter(asList(Table.values()), t -> t.view));
    }
}
