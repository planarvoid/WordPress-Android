package com.soundcloud.android.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Table {
    TRACKS("Tracks", false, DBHelper.DATABASE_CREATE_TRACKS, DBHelper.Tracks.ALL_FIELDS),
    TRACK_PLAYS("TrackPlays", false, null),
    TRACK_METADATA("TrackMetadata", false, DBHelper.DATABASE_CREATE_TRACK_METADATA, DBHelper.TrackMetadata.ALL_FIELDS),
    USERS("Users", false, DBHelper.DATABASE_CREATE_USERS, DBHelper.Users.ALL_FIELDS),
    COMMENTS("Comments", false, DBHelper.DATABASE_CREATE_COMMENTS),
    ACTIVITIES("Activities", false, DBHelper.DATABASE_CREATE_ACTIVITIES),
    RECORDINGS("Recordings", false, DBHelper.DATABASE_CREATE_RECORDINGS, DBHelper.Recordings.ALL_FIELDS),
    SEARCHES("Searches", false, DBHelper.DATABASE_CREATE_SEARCHES),

    PLAYLIST("Playlist", false, DBHelper.DATABASE_CREATE_PLAYLIST),
    PLAYLIST_ITEMS("PlaylistItems", false, DBHelper.DATABASE_CREATE_PLAYLIST_ITEMS),

    COLLECTION_ITEMS("CollectionItems", false, DBHelper.DATABASE_CREATE_COLLECTION_ITEMS),
    COLLECTIONS("Collections", false, DBHelper.DATABASE_CREATE_COLLECTIONS),
    COLLECTION_PAGES("CollectionPages", false, DBHelper.DATABASE_CREATE_COLLECTION_PAGES),

    // views
    TRACK_VIEW("TrackView", true, DBHelper.DATABASE_CREATE_TRACK_VIEW),
    ACTIVITY_VIEW("ActivityView", true, DBHelper.DATABASE_CREATE_ACTIVITY_VIEW),
    ;


    public final String name;
    public final String createString;
    public final String id;
    public final String[] fields;
    public final boolean view;
    public static final String TAG = DBHelper.TAG;

    Table(String name, boolean view, String create, String... fields) {
        if (name == null) throw new NullPointerException();
        this.name = name;
        this.view = view;
        if (create != null) {
            createString = buildCreateString(name,create,view);
        } else {
            createString = null;
        }
        id = this.name +"."+BaseColumns._ID;
        this.fields = fields;
    }

    public static String buildCreateString(String tableName, String columnString, boolean isView){
        return "CREATE "+(isView ? "VIEW" : "TABLE")+" IF NOT EXISTS "+tableName+" "+columnString;
    }

    public String allFields() {
        return this.name+".*";
    }

    public String field(String field) {
        return this.name+"."+field;
    }

    public static Table get(String name) {
        for (Table table : values()) {
            if (table.name.equals(name)) return table;
        }
        return null;
    }

    public void drop(SQLiteDatabase db) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "dropping " + name);
        db.execSQL("DROP " + (view ? "VIEW" : "TABLE") +" IF EXISTS "+name);
    }

    public boolean exists(SQLiteDatabase db) {
        try {
            db.execSQL("SELECT 1 FROM "+name);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void create(SQLiteDatabase db) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "creating " + name);
        if (!TextUtils.isEmpty(createString)) {
            db.execSQL(createString);
        } else if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "NOT creating " + name);
    }

    public void recreate(SQLiteDatabase db) {
        drop(db);
        create(db);
    }

    public List<String> getColumnNames(SQLiteDatabase db) {
        return getColumnNames(db, name);
    }

    public static List<String> getColumnNames(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("PRAGMA table_info (" + table + ")", null);
        List<String> cols = new ArrayList<String>();
        while (cursor != null && cursor.moveToNext()) {
            cols.add(cursor.getString(1));
        }
        if (cursor != null) cursor.close();
        return cols;
    }

    public List<String> alterColumns(SQLiteDatabase db) {
        return alterColumns(db, new String[0], new String[0]);
    }

    public List<String> alterColumns(SQLiteDatabase db, @Nullable String[] fromAppendCols, @Nullable String[] toAppendCols) {
        return alterColumns(db, name, createString, fromAppendCols, toAppendCols);
    }

    public static List<String> alterColumns(SQLiteDatabase db,
                                            final String table,
                                            final String createString,
                                            final String[] fromAppend,
                                            final String[] toAppend) {
        List<String> toAppendCols = new ArrayList<String>();
        List<String> fromAppendCols = new ArrayList<String>();
        Collections.addAll(fromAppendCols, fromAppend);
        Collections.addAll(toAppendCols, toAppend);

        final String tmpTable = "bck_"+table;
        db.execSQL("DROP TABLE IF EXISTS "+tmpTable);

        // create tmp table with current schema
        db.execSQL(createString.replace(" "+table+" ", " "+tmpTable+" "));

        // get list of columns defined in new schema
        List<String> columns = getColumnNames(db, tmpTable);

        // only keep columns which are in current schema
        columns.retainAll(getColumnNames(db, table));

        toAppendCols.addAll(columns);
        fromAppendCols.addAll(columns);

        final String toCols   = TextUtils.join(",", toAppendCols);
        final String fromCols = TextUtils.join(",", fromAppendCols);

        // copy current data to tmp table
        final String sql = String.format("INSERT INTO %s (%s) SELECT %s from %s", tmpTable, toCols, fromCols, table);

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "executing "+sql);
        db.execSQL(sql);

        // recreate current table with new schema
        db.execSQL("DROP TABLE IF EXISTS "+table);
        db.execSQL(createString);

        // and copy old data from tmp
        final String copy = String.format("INSERT INTO %s (%s) SELECT %s from %s", table, toCols, toCols, tmpTable);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "executing "+copy);
        db.execSQL(copy);
        db.execSQL("DROP table "+tmpTable);

        return toAppendCols;
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/418898/sqlite-upsert-not-insert-or-replace/">
     *  SQLite - UPSERT *not* INSERT or REPLACE
     * </a>
     */

    public int upsert(SQLiteDatabase db, ContentValues[] values) {
        if (fields == null || fields.length == 0) {
            throw new IllegalStateException("no fields defined");
        }
        db.beginTransaction();
        for (ContentValues v : values)  {
            long id = v.getAsLong(BaseColumns._ID);
            List<Object> bindArgs = new ArrayList<Object>();
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT OR REPLACE INTO ").append(name).append("(")
                    .append(TextUtils.join(",", fields))
                    .append(") VALUES (");
            for (int i = 0; i < fields.length; i++) {
                String f = fields[i];
                if (v.containsKey(f)) {
                    sb.append("?");
                    bindArgs.add(v.get(f));
                } else {
                    sb.append("(SELECT ").append(f).append(" FROM ").append(name).append(" WHERE _id=?)");
                    bindArgs.add(id);
                }
                if (i < fields.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(");");
            final String sql = sb.toString();
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "sql:"+sql);
            db.execSQL(sql, bindArgs.toArray());
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return values.length;
    }

    public int upsertSingle(SQLiteDatabase db, ContentValues cv) {
        return upsert(db, new ContentValues[] { cv } );
    }

    public int upsertSingleArgs(SQLiteDatabase db, Object... args) {
        return upsertSingle(db, build(args));
    }

    public long insertOrReplace(SQLiteDatabase db, ContentValues cv) {
        return insertWithOnConflict(db, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @SuppressLint("NewApi")
    public long insertWithOnConflict(SQLiteDatabase db, ContentValues cv, int conflict) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
            return db.insertWithOnConflict(name, null, cv, conflict);
        } else {
            // 2.1 compatible code
            switch (conflict) {
                case SQLiteDatabase.CONFLICT_REPLACE: {
                    final long id = db.insert(name, null, cv);
                    if (id == -1) {
                        Long lid = cv.getAsLong(BaseColumns._ID);
                        if (lid != null) {
                            if (db.update(name, cv, "_id = ?", new String[]{String.valueOf(lid)}) == 1) {
                                return lid;
                            } else {
                                return -1;
                            }
                        } else return -1;
                    } else return id;
                }

                default:
                    return -1;
            }
        }
    }

    public long insertOrReplaceArgs(SQLiteDatabase db, Object... args) {
        return insertOrReplace(db, build(args));
    }

    public static @NotNull ContentValues build(@NotNull Object... args) {
        ContentValues cv = new ContentValues();
        if (args.length % 2 != 0) throw new IllegalArgumentException("need even number of arguments");
        for (int i = 0; i < args.length; i += 2) {
            final Object obj = args[i+1];
            final String key = args[i].toString();
            if (obj instanceof String) {
                cv.put(key, (String)obj);
            } else if (obj instanceof Double) {
                cv.put(key, (Double)obj);
            } else if (obj instanceof Integer) {
                cv.put(key, (Integer)obj);
            } else if (obj instanceof Long) {
                cv.put(key, (Long)obj);
            } else if (obj instanceof Boolean) {
                cv.put(key, (Boolean)obj);
            } else if (obj instanceof Float) {
                cv.put(key, (Float)obj);
            } else {
                Log.w(TAG, "unknown obj: "+obj);
            }
        }
        return cv;
    }

    public static String schemaSnapshot() {
        StringBuilder sb = new StringBuilder();
        for (Table t : values()) {
            if (t.createString != null) {
                sb.append(t.createString);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
