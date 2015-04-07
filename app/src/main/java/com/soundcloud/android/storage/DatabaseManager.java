package com.soundcloud.android.storage;

import static com.soundcloud.android.storage.Table.migrate;

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
    public static final int DATABASE_VERSION = 40;
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
            Table.Posts.create(db);
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
            Table.OfflineContent.create(db);
            Table.TrackPolicies.create(db);
            migratePolicies(db);

            Table.Sounds.alterColumns(db);
            Table.SoundView.recreate(db);
            Table.SoundAssociationView.recreate(db);
            Table.PlaylistTracksView.recreate(db);
            Table.SoundStreamView.recreate(db);
            Table.ActivityView.recreate(db);
            
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
            Table.Posts.recreate(db);
            return true;
        } catch (SQLException exception) {
            handleUpgradeException(exception, oldVersion, 40);
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

        migrate(db, Table.TrackPolicies.name(), newPoliciesColumns, Table.Sounds.name(), oldSoundColumns);
    }

    private static void handleUpgradeException(SQLException exception, int oldVersion, int newVersion) {
        final String message =
                String.format(Locale.US, "error during upgrade%d (from %d)", newVersion, oldVersion);
        ErrorUtils.handleSilentException(message, exception);
    }
}
