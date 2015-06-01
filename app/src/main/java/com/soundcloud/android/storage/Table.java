package com.soundcloud.android.storage;

import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum Table implements com.soundcloud.propeller.Table {
    SoundStream(false, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM),
    PromotedTracks(false, DatabaseSchema.DATABASE_CREATE_PROMOTED_TRACKS),
    Sounds(PrimaryKey.of(TableColumns.Sounds._ID, TableColumns.Sounds._TYPE), false, DatabaseSchema.DATABASE_CREATE_SOUNDS, TableColumns.Sounds.ALL_FIELDS),
    TrackMetadata(false, DatabaseSchema.DATABASE_CREATE_TRACK_METADATA, TableColumns.TrackMetadata.ALL_FIELDS),
    TrackPolicies(PrimaryKey.of(TableColumns.TrackPolicies.TRACK_ID), false, DatabaseSchema.DATABASE_CREATE_TRACK_POLICIES, TableColumns.TrackPolicies.ALL_FIELDS),
    Users(false, DatabaseSchema.DATABASE_CREATE_USERS, TableColumns.Users.ALL_FIELDS),
    Comments(false, DatabaseSchema.DATABASE_CREATE_COMMENTS),
    Activities(false, DatabaseSchema.DATABASE_CREATE_ACTIVITIES),
    Recordings(false, DatabaseSchema.DATABASE_CREATE_RECORDINGS, TableColumns.Recordings.ALL_FIELDS),
    Searches(false, DatabaseSchema.DATABASE_CREATE_SEARCHES),
    PlaylistTracks(PrimaryKey.of(
            TableColumns.PlaylistTracks._ID,
            TableColumns.PlaylistTracks.POSITION,
            TableColumns.PlaylistTracks.PLAYLIST_ID),
            false, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS),
    UserAssociations(false, DatabaseSchema.DATABASE_CREATE_USER_ASSOCIATIONS),

    PlayQueue(false, DatabaseSchema.DATABASE_CREATE_PLAY_QUEUE),

    CollectionItems(PrimaryKey.of(
            TableColumns.CollectionItems.USER_ID,
            TableColumns.CollectionItems.ITEM_ID,
            TableColumns.CollectionItems.COLLECTION_TYPE,
            TableColumns.CollectionItems.RESOURCE_TYPE),
            false, DatabaseSchema.DATABASE_CREATE_COLLECTION_ITEMS),
    Collections(false, DatabaseSchema.DATABASE_CREATE_COLLECTIONS),
    CollectionPages(PrimaryKey.of(
            TableColumns.CollectionPages.COLLECTION_ID,
            TableColumns.CollectionPages.PAGE_INDEX),
            false, DatabaseSchema.DATABASE_CREATE_COLLECTION_PAGES),

    Suggestions(false, DatabaseSchema.DATABASE_CREATE_SUGGESTIONS, TableColumns.Suggestions.ALL_FIELDS),

    TrackDownloads(false, DatabaseSchema.DATABASE_CREATE_TRACK_DOWNLOADS), // download state
    OfflineContent(false, DatabaseSchema.DATABASE_CREATE_OFFLINE_CONTENT), // marked for offline sync (user intent)

    Likes(PrimaryKey.of(TableColumns.Likes._ID, TableColumns.Likes._TYPE), false, DatabaseSchema.DATABASE_CREATE_LIKES),
    Posts(PrimaryKey.of(TableColumns.Posts.TYPE, TableColumns.Posts.TARGET_TYPE, TableColumns.Posts.TARGET_ID), false, DatabaseSchema.DATABASE_CREATE_POSTS),

    // views
    SoundView(true, DatabaseSchema.DATABASE_CREATE_SOUND_VIEW),
    SoundStreamView(true, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM_VIEW),
    ActivityView(true, DatabaseSchema.DATABASE_CREATE_ACTIVITY_VIEW),
    SoundAssociationView(true, DatabaseSchema.DATABASE_CREATE_SOUND_ASSOCIATION_VIEW),
    UserAssociationView(PrimaryKey.of(
            TableColumns.UserAssociations.OWNER_ID,
            TableColumns.UserAssociations.TARGET_ID,
            TableColumns.UserAssociations.ASSOCIATION_TYPE,
            TableColumns.UserAssociations.RESOURCE_TYPE),
            true, DatabaseSchema.DATABASE_CREATE_USER_ASSOCIATION_VIEW),
    PlaylistTracksView(true, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS_VIEW);


    public final PrimaryKey primaryKey;
    public final String createString;
    public final String id;
    public final String type;
    public final String[] fields;
    public final boolean view;
    public static final String TAG = DatabaseManager.TAG;

    Table(boolean view, String create, String... fields) {
        this(PrimaryKey.of(BaseColumns._ID), view, create, fields);
    }

    Table(PrimaryKey primaryKey, boolean view, String create, String... fields) {
        this.primaryKey = primaryKey;
        this.view = view;
        if (create != null) {
            createString = buildCreateString(name(), create, view);
        } else {
            createString = null;
        }
        id = this.name() + "." + BaseColumns._ID;
        type = this.name() + "." + TableColumns.ResourceTable._TYPE;
        this.fields = fields;
    }

    public static String buildCreateString(String tableName, String columnString, boolean isView) {
        return "CREATE " + (isView ? "VIEW" : "TABLE") + " IF NOT EXISTS " + tableName + " " + columnString;
    }

    @Override
    public PrimaryKey primaryKey() {
        return primaryKey;
    }

    public String allFields() {
        return this.name() + ".*";
    }

    public String field(String field) {
        return this.name() + "." + field;
    }

    public static Table get(String name) {
        for (Table table : values()) {
            if (table.name().equals(name)) {
                return table;
            }
        }
        return null;
    }

    public void drop(SQLiteDatabase db) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "dropping " + name());
        }
        db.execSQL("DROP " + (view ? "VIEW" : "TABLE") + " IF EXISTS " + name());
    }

    public boolean exists(SQLiteDatabase db) {
        try {
            db.execSQL("SELECT 1 FROM " + name());
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void create(SQLiteDatabase db) {
        if (!TextUtils.isEmpty(createString)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "creating " + (view ? "view" : "table") + " " + name());
            }
            db.execSQL(createString);
        } else if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "NOT creating " + name());
        }
    }

    public void recreate(SQLiteDatabase db) {
        drop(db);
        create(db);
    }

    public List<String> getColumnNames(SQLiteDatabase db) {
        return getColumnNames(db, name());
    }

    public static List<String> getColumnNames(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("PRAGMA table_info (" + table + ")", null);
        List<String> cols = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            cols.add(cursor.getString(1));
        }
        if (cursor != null) {
            cursor.close();
        }
        return cols;
    }

    public List<String> alterColumns(SQLiteDatabase db) {
        return alterColumns(db, new String[0], new String[0]);
    }

    public List<String> alterColumns(SQLiteDatabase db, String[] fromAppendCols, String[] toAppendCols) {
        return alterColumns(db, name(), createString, fromAppendCols, toAppendCols);
    }

    @Deprecated // should be a private method
    public static List<String> alterColumns(SQLiteDatabase db,
                                            final String table,
                                            final String createString,
                                            final String[] fromAppend,
                                            final String[] toAppend) {
        List<String> toAppendCols = new ArrayList<>();
        List<String> fromAppendCols = new ArrayList<>();
        java.util.Collections.addAll(fromAppendCols, fromAppend);
        java.util.Collections.addAll(toAppendCols, toAppend);

        final String tmpTable = "bck_" + table;
        db.execSQL("DROP TABLE IF EXISTS " + tmpTable);

        // create tmp table with current schema
        db.execSQL(createString.replace(" " + table + " ", " " + tmpTable + " "));

        // get list of columns defined in new schema
        List<String> columns = getColumnNames(db, tmpTable);

        // only keep columns which are in current schema
        columns.retainAll(getColumnNames(db, table));

        toAppendCols.addAll(columns);
        fromAppendCols.addAll(columns);

        final String toCols = TextUtils.join(",", toAppendCols);
        final String fromCols = TextUtils.join(",", fromAppendCols);

        // copy current data to tmp table
        final String sql = String.format(Locale.ENGLISH, "INSERT INTO %s (%s) SELECT %s from %s", tmpTable, toCols, fromCols, table);

        Log.d(TAG, "executing " + sql);
        db.execSQL(sql);

        // recreate current table with new schema
        db.execSQL("DROP TABLE IF EXISTS " + table);
        db.execSQL(createString);

        // and copy old data from tmp
        final String copy = String.format(Locale.ENGLISH, "INSERT INTO %s (%s) SELECT %s from %s", table, toCols, toCols, tmpTable);

        Log.d(TAG, "executing " + copy);
        db.execSQL(copy);
        db.execSQL("DROP table " + tmpTable);

        return toAppendCols;
    }

    public static void migrate(SQLiteDatabase db, String toTable, List<String> toColumns,
                               String fromTable, List<String> fromColumns) {
        final String toCols = TextUtils.join(",", toColumns);
        final String fromCols = TextUtils.join(",", fromColumns);
        final String sql = String.format(Locale.ENGLISH, "INSERT INTO %s (%s) SELECT %s from %s", toTable, toCols, fromCols, fromTable);

        Log.d(TAG, "migrating " + sql);
        db.execSQL(sql);
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/418898/sqlite-upsert-not-create-or-replace/">
     * SQLite - UPSERT *not* INSERT or REPLACE
     * </a>
     */
    public int upsert(SQLiteDatabase db, ContentValues[] values) {
        if (fields == null || fields.length == 0) {
            throw new IllegalStateException("no fields defined");
        }
        db.beginTransaction();
        int updated = 0;
        for (ContentValues v : values) {
            if (v == null) {
                continue;
            }
            long id = v.getAsLong(BaseColumns._ID);
            List<Object> bindArgs = new ArrayList<>();
            StringBuilder sb = new StringBuilder(5000);
            sb.append("INSERT OR REPLACE INTO ").append(name()).append('(')
                    .append(TextUtils.join(",", fields))
                    .append(") VALUES (");
            for (int i = 0; i < fields.length; i++) {
                String f = fields[i];
                if (v.containsKey(f)) {
                    sb.append('?');
                    bindArgs.add(v.get(f));
                } else {
                    sb.append("(SELECT ").append(f).append(" FROM ").append(name()).append(" WHERE _id=?)");
                    bindArgs.add(id);
                }
                if (i < fields.length - 1) {
                    sb.append(',');
                }
            }
            sb.append(");");
            final String sql = sb.toString();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "sql:" + sql);
            }
            db.execSQL(sql, bindArgs.toArray());
            updated++;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return updated;
    }

    public long upsertSingle(SQLiteDatabase db, ContentValues cv) {
        upsert(db, new ContentValues[]{cv});
        return cv.getAsLong(BaseColumns._ID);
    }

    public long insertOrReplace(SQLiteDatabase db, ContentValues cv) {
        return insertWithOnConflict(db, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long insertWithOnConflict(SQLiteDatabase db, ContentValues cv, int conflict) {
        return db.insertWithOnConflict(name(), null, cv, conflict);
    }

    @NotNull
    public static ContentValues build(@NotNull Object... args) {
        ContentValues cv = new ContentValues();
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("need even number of arguments");
        }
        for (int i = 0; i < args.length; i += 2) {
            final Object obj = args[i + 1];
            final String key = args[i].toString();
            if (obj instanceof String) {
                cv.put(key, (String) obj);
            } else if (obj instanceof Double) {
                cv.put(key, (Double) obj);
            } else if (obj instanceof Integer) {
                cv.put(key, (Integer) obj);
            } else if (obj instanceof Long) {
                cv.put(key, (Long) obj);
            } else if (obj instanceof Boolean) {
                cv.put(key, (Boolean) obj);
            } else if (obj instanceof Float) {
                cv.put(key, (Float) obj);
            } else {
                Log.w(TAG, "unknown obj: " + obj);
            }
        }
        return cv;
    }

    public static String schemaSnapshot() {
        StringBuilder sb = new StringBuilder();
        for (Table t : values()) {
            if (t.createString != null) {
                sb.append(t.createString);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name();
    }
}
