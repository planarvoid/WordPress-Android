package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.LocalCollection;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

public class DatabaseManager extends SQLiteOpenHelper {
    /* package */ static final String TAG = "DatabaseManager";

    /* increment when schema changes */
    public static final int DATABASE_VERSION = 26;
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
        Log.d(TAG, "onCreate(" + db + "");

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
                        case 4:
                            success = upgradeTo4(db, oldVersion);
                            break;
                        case 5:
                            success = upgradeTo5(db, oldVersion);
                            break;
                        case 6:
                            success = upgradeTo6(db, oldVersion);
                            break;
                        case 7:
                            success = upgradeTo7(db, oldVersion);
                            break;
                        case 8:
                            success = upgradeTo8(db, oldVersion);
                            break;
                        case 9:
                            success = upgradeTo9(db, oldVersion);
                            break;
                        case 10:
                            success = upgradeTo10(db, oldVersion);
                            break;
                        case 11:
                            success = upgradeTo11(db, oldVersion);
                            break;
                        case 12:
                            success = upgradeTo12(db, oldVersion);
                            break;
                        case 13:
                            success = upgradeTo13(db, oldVersion);
                            break;
                        case 14:
                            success = upgradeTo14(db, oldVersion);
                            break;
                        case 15:
                            success = upgradeTo15(db, oldVersion);
                            break;
                        case 16:
                        case 17:
                        case 18:
                            success = true;
                            break;
                        case 19:
                            success = upgradeTo19(db, oldVersion);
                            break;
                        case 20:
                            success = upgradeTo20(db, oldVersion);
                            break;
                        case 21:
                            success = upgradeTo21(db, oldVersion);
                            break;
                        case 22:
                            success = upgradeTo22(db, oldVersion);
                            break;
                        case 23:
                            success = upgradeTo23(db, oldVersion);
                            break;
                        case 24:
                            success = upgradeTo24(db, oldVersion);
                            break;
                        case 25:
                            success = upgradeTo25(db, oldVersion);
                            break;
                        case 26:
                            success = upgradeTo26(db, oldVersion);
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
        Log.d(TAG, "onRecreate(" + db + "");

        for (Table t : Table.values()) {
            t.drop(db);
        }
        onCreate(db);
    }


    /*
    * altered id naming for content resolver
    */
    private static boolean upgradeTo4(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db, new String[]{"id"}, new String[]{"_id"});
            Table.USERS.alterColumns(db, new String[]{"id"}, new String[]{"_id"});
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade4 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /*
     * added sharing to database
     */
    private static boolean upgradeTo5(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db, null, null);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade5 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /*
     * added sharing to database
     */
    private static boolean upgradeTo6(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.create(db);
            Table.SOUNDS.alterColumns(db);
            Table.USERS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade6 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }


    private static boolean upgradeTo7(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade7 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo8(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SEARCHES.create(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade8 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo9(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db);
            Table.USERS.alterColumns(db);

            // trackview refers to metadata now (http://www.bugsense.com/dashboard/project/806c72af#error/24301879)
            Table.TRACK_METADATA.create(db);

            Table.SOUND_VIEW.create(db);
            Table.COMMENTS.create(db);
            Table.ACTIVITIES.create(db);
            Table.ACTIVITY_VIEW.create(db);
            Table.COLLECTIONS.create(db);
            Table.COLLECTION_PAGES.create(db);
            Table.COLLECTION_ITEMS.create(db);
            Table.PLAY_QUEUE.create(db);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade9 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo10(SQLiteDatabase db, int oldVersion) {
        try {
            Table.TRACK_METADATA.create(db);
            Table.SOUND_VIEW.recreate(db);
            Table.USERS.alterColumns(db);
            return true;

        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade10 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    /**
     * Fix for incorrect future-href. Have to clean out activities and stored future-href.
     */
    private static boolean upgradeTo11(SQLiteDatabase db, int oldVersion) {
        try {
            cleanActivities(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade11 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo12(SQLiteDatabase db, int oldVersion) {
        try {
            cleanActivities(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade12 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo13(SQLiteDatabase db, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(db);
            Table.SOUND_VIEW.recreate(db);
            Table.COLLECTIONS.alterColumns(db);
            Table.RECORDINGS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade13 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo14(SQLiteDatabase db, int oldVersion) {
        try {
            resetSyncState(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade14 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }


    // Schema used in 2.3.2
    private static boolean upgradeTo15(SQLiteDatabase db, int oldVersion) {
        try {
            Table.RECORDINGS.alterColumns(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade15 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }


    // Schema used in 2.4.0
    private static boolean upgradeTo19(SQLiteDatabase db, int oldVersion) {
        try {
            // legacy tables
            db.execSQL("DROP TABLE IF EXISTS PlaylistItems");
            db.execSQL("DROP TABLE IF EXISTS Playlist");
            db.execSQL("DROP TABLE IF EXISTS Tracks");
            db.execSQL("DROP TABLE IF EXISTS TrackPlays");
            db.execSQL("DROP VIEW  IF EXISTS TrackView");

            for (Table t : Table.values()) {
                if (t == Table.RECORDINGS) continue;
                t.recreate(db);
            }
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade19" +
                    "(from " + oldVersion + ")", e);
            return false;
        }

    }

    // Schema used in sets, added extra fields to SoundView
    private static boolean upgradeTo20(SQLiteDatabase db, int oldVersion) {
        try {
            Table.COLLECTIONS.recreate(db);
            Table.COLLECTION_ITEMS.recreate(db);
            Table.SOUNDS.recreate(db);
            Table.ACTIVITIES.recreate(db);
            Table.PLAYLIST_TRACKS.recreate(db);

            Table.SOUND_VIEW.recreate(db);
            Table.SOUND_ASSOCIATION_VIEW.recreate(db);
            Table.ACTIVITY_VIEW.recreate(db);
            Table.PLAYLIST_TRACKS_VIEW.recreate(db);

            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade20 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    // Post sets beta. added sound_type to ActivityView
    private static boolean upgradeTo21(SQLiteDatabase db, int oldVersion) {
        try {
            Table.ACTIVITY_VIEW.recreate(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade21 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    // deduplicate logic in schema
    private static boolean upgradeTo22(SQLiteDatabase db, int oldVersion) {
        return upgradeTo21(db, oldVersion);
    }

    // Moved UserAssociations to new table, added User Associations view and refactored association views in general
    private static boolean upgradeTo23(SQLiteDatabase db, int oldVersion) {
        try {
            Table.USER_ASSOCIATIONS.recreate(db);
            String[] toAppendCols = new String[]{
                    TableColumns.UserAssociations.OWNER_ID,
                    TableColumns.UserAssociations.TARGET_ID,
                    TableColumns.UserAssociations.RESOURCE_TYPE,
                    TableColumns.UserAssociations.ASSOCIATION_TYPE,
                    TableColumns.UserAssociations.POSITION,
                    TableColumns.UserAssociations.CREATED_AT
            };
            String[] fromAppendCols = new String[]{
                    TableColumns.CollectionItems.USER_ID,
                    TableColumns.CollectionItems.ITEM_ID,
                    TableColumns.CollectionItems.RESOURCE_TYPE,
                    TableColumns.CollectionItems.COLLECTION_TYPE,
                    TableColumns.CollectionItems.POSITION,
                    TableColumns.CollectionItems.CREATED_AT
            };
            String[] userTypes = new String[]{
                    String.valueOf(CollectionStorage.CollectionItemTypes.FOLLOWER),
                    String.valueOf(CollectionStorage.CollectionItemTypes.FOLLOWING),
                    String.valueOf(CollectionStorage.CollectionItemTypes.FRIEND)
            };

            String sql = String.format(Locale.ENGLISH,
                    "INSERT INTO %s (%s) SELECT %s from %s where %s in (%s)",
                    Table.USER_ASSOCIATIONS.name,
                    TextUtils.join(",", toAppendCols),
                    TextUtils.join(",", fromAppendCols),
                    Table.COLLECTION_ITEMS.name,
                    TableColumns.CollectionItems.COLLECTION_TYPE,
                    TextUtils.join(",", userTypes));

            db.execSQL(sql);

            int moved = db.delete(Table.COLLECTION_ITEMS.name,
                    ResolverHelper.getWhereInClause(TableColumns.CollectionItems.COLLECTION_TYPE, userTypes.length),
                    userTypes);

            Log.d(TAG, "Moved " + moved + " associations to the UserAssociations table during upgrade");

            Table.SOUND_ASSOCIATION_VIEW.recreate(db);
            Table.USER_ASSOCIATION_VIEW.recreate(db);

            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade23 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    // Explore version. Includes PlayQueue refactoring and prep for eventlogger source tags
    private static boolean upgradeTo24(SQLiteDatabase db, int oldVersion) {
        try {
            Table.PLAY_QUEUE.recreate(db);
            return true;
        } catch (SQLException e) {
            SoundCloudApplication.handleSilentException("error during upgrade24 " +
                    "(from " + oldVersion + ")", e);
        }
        return false;
    }

    private static boolean upgradeTo25(SQLiteDatabase database, int oldVersion) {
        try {
            Table.ACTIVITIES.recreate(database);
            return true;
        } catch (SQLException exception) {
            SoundCloudApplication.handleSilentException("error during upgrade25 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    private static boolean upgradeTo26(SQLiteDatabase database, int oldVersion) {
        try {
            Table.SOUNDS.alterColumns(database);
            return true;
        } catch (SQLException exception) {
            SoundCloudApplication.handleSilentException("error during upgrade26 " +
                    "(from " + oldVersion + ")", exception);
        }
        return false;
    }

    private static void cleanActivities(SQLiteDatabase db) {
        Table.ACTIVITIES.recreate(db);
        db.execSQL("UPDATE " + Table.COLLECTIONS + " SET " + TableColumns.Collections.EXTRA + " = NULL");
    }

    private static void resetSyncState(SQLiteDatabase db) {
        db.execSQL("UPDATE " + Table.COLLECTIONS + " SET " + TableColumns.Collections.SYNC_STATE + " =" + LocalCollection.SyncState.IDLE);
    }
}
