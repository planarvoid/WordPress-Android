package com.soundcloud.android.storage;

import com.soundcloud.android.utils.ErrorUtils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.Locale;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"}) // We know
public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 39;
    private static final String DATABASE_NAME = "SoundCloud";

    private static DatabaseManager instance;

    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    public static long getDatabaseFileSize(Context context) {
        final File databasePath = context.getDatabasePath(DATABASE_NAME);
        return databasePath != null ? databasePath.length() : -1L;
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
            for (Table t : Table.values()) {
                t.create(db);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (newVersion > oldVersion) {
            db.beginTransaction();
            boolean success = false;
            if (oldVersion >= 3) {
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 24:
                            success = upgradeTo24(db, oldVersion);
                            break;
                        case 25:
                            success = upgradeTo25(db, oldVersion);
                            break;
                        case 26:
                            success = upgradeTo26(db, oldVersion);
                            break;
                        case 27:
                            success = upgradeTo27(db, oldVersion);
                            break;
                        case 28:
                            success = upgradeTo28(db, oldVersion);
                            break;
                        case 29:
                            success = upgradeTo29(db, oldVersion);
                            break;
                        case 30:
                            success = upgradeTo30(db, oldVersion);
                            break;
                        case 31:
                            success = upgradeTo31(db, oldVersion);
                            break;
                        case 32:
                            success = upgradeTo32(db, oldVersion);
                            break;
                        case 33:
                            success = upgradeTo33(db, oldVersion);
                            break;
                        case 34:
                            success = upgradeTo34(db, oldVersion);
                            break;
                        case 35:
                            success = upgradeTo35(db, oldVersion);
                            break;
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

    public void onRecreateDb(SQLiteDatabase db) {
        Log.d(TAG, "onRecreate(" + db + ")");

        for (Table t : Table.values()) {
            t.drop(db);
        }
        onCreate(db);
    }

    // Explore version. Includes PlayQueue refactoring and prep for eventlogger source tags
    private static boolean upgradeTo24(SQLiteDatabase db, int oldVersion) {
        try {
            Table.PlayQueue.recreate(db);
            return true;
        } catch (SQLException e) {
            handleUpgradeException(e, oldVersion, 24);
        }
        return false;
    }

    private static boolean upgradeTo25(SQLiteDatabase database, int oldVersion) {
        try {
            Table.Activities.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 25);
        }
        return false;
    }

    private static boolean upgradeTo26(SQLiteDatabase database, int oldVersion) {
        try {
            Table.Sounds.alterColumns(database);
            Table.SoundView.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 26);
        }
        return false;
    }

    /**
     * Made SoundAssociationView inner join
     */
    private static boolean upgradeTo27(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SoundAssociationView.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 27);
        }
        return false;
    }

    /**
     * Added policy, monetizable for new player / audio ads
     */
    private static boolean upgradeTo28(SQLiteDatabase database, int oldVersion) {
        try {
            Table.Sounds.alterColumns(database);
            Table.SoundView.recreate(database);
            Table.ActivityView.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 28);
        }
        return false;
    }

    /**
     * Added description to track table
     */
    private static boolean upgradeTo29(SQLiteDatabase database, int oldVersion) {
        try {
            Table.Sounds.alterColumns(database);
            Table.SoundView.recreate(database);
            Table.ActivityView.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 29);
        }
        return false;
    }

    /**
     * New SoundStream syncing + storage
     */
    private static boolean upgradeTo30(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SoundStream.create(database);
            Table.SoundStreamView.create(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 30);
        }
        return false;
    }

    /**
     * Added Track Downloads table
     */
    private static boolean upgradeTo31(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TrackDownloads.create(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 31);
        }
        return false;
    }

    /**
     * Added Likes table
     */
    private static boolean upgradeTo32(SQLiteDatabase db, int oldVersion) {
        try {
            Table.Likes.create(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 32);
        }
        return false;
    }

    /**
     * Added promoted tracks
     */
    private static boolean upgradeTo33(SQLiteDatabase database, int oldVersion) {
        try {
            Table.PromotedTracks.create(database);
            Table.Activities.alterColumns(database);
            Table.ActivityView.recreate(database);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 33);
        }
        return false;
    }

    /**
     * Added added_at column to Likes table
     */
    private static boolean upgradeTo34(SQLiteDatabase db, int oldVersion) {
        try {
            Table.Likes.alterColumns(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 34);
        }
        return false;
    }

    /**
     * Added removed_at column to TrackDownloads table
     * Recreate SoundView and descendents after adding downloaded_at and removed_at
     */
    private static boolean upgradeTo35(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TrackDownloads.recreate(db);
            Table.SoundView.recreate(db);
            Table.SoundAssociationView.recreate(db);
            Table.PlaylistTracksView.recreate(db);
            Table.SoundStreamView.recreate(db);
            Table.ActivityView.recreate(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 35);
        }
        return false;
    }

    /**
     * Added unavailable_at column to TrackDownloads table
     */
    private static boolean upgradeTo36(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TrackDownloads.recreate(db);
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
            Table.PlaylistTracks.alterColumns(db);
        }catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 37);
        }
        return false;
    }

    /**
     * Added Posts table
     */
    private static boolean upgradeTo38(SQLiteDatabase db, int oldVersion) {
        try {
            Table.Posts.create(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 38);
        }
        return false;
    }

    /**
     * Added OfflineContent table
     */
    private static boolean upgradeTo39(SQLiteDatabase db, int oldVersion) {
        try {
            Table.OfflineContent.create(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 39);
        }
        return false;
    }

    private static void handleUpgradeException(SQLException exception, int oldVersion, int newVersion) {
        final String message =
                String.format(Locale.US, "error during upgrade%d (from %d)", newVersion, oldVersion);
        ErrorUtils.handleSilentException(message, exception);
    }
}
