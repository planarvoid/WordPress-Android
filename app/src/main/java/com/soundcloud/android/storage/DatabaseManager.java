package com.soundcloud.android.storage;

import com.soundcloud.android.utils.ErrorUtils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"}) // We know
public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 29;
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
            Table.PLAY_QUEUE.recreate(db);
            return true;
        } catch (SQLException e) {
            ErrorUtils.handleSilentException("error during upgrade24 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo25(SQLiteDatabase database, int oldVersion) {
        try {
            Table.ACTIVITIES.recreate(database);
            return true;
        } catch (SQLException exception) {
            ErrorUtils.handleSilentException("error during upgrade25 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    private static boolean upgradeTo26(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(database);
            Table.SOUND_VIEW.recreate(database);
            return true;
        } catch (SQLException exception) {
            ErrorUtils.handleSilentException("error during upgrade26 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    // made SoundAssiciationView inner join
    private static boolean upgradeTo27(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SOUND_ASSOCIATION_VIEW.recreate(database);
            return true;
        } catch (SQLException exception) {
            ErrorUtils.handleSilentException("error during upgrade27 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    // added policy, monetizable for new player / audio ads
    private static boolean upgradeTo28(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(database);
            Table.SOUND_VIEW.recreate(database);
            Table.ACTIVITY_VIEW.recreate(database);
            return true;
        } catch (SQLException exception) {
            ErrorUtils.handleSilentException("error during upgrade28 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    // added description to track table
    private static boolean upgradeTo29(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(database);
            Table.SOUND_VIEW.recreate(database);
            Table.ACTIVITY_VIEW.recreate(database);
            return true;
        } catch (SQLException exception) {
            ErrorUtils.handleSilentException("error during upgrade29 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }
}
