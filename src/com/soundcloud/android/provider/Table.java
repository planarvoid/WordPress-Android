package com.soundcloud.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public enum Table {
    TRACKS("Tracks", DBHelper.DATABASE_CREATE_TRACKS, DBHelper.Tracks.ALL_FIELDS),
    TRACK_PLAYS("TrackPlays", ""),
    TRACK_METADATA("TrackMetadata", DBHelper.DATABASE_CREATE_TRACK_METADATA, DBHelper.TrackMetadata.ALL_FIELDS),
    USERS("Users", DBHelper.DATABASE_CREATE_USERS, DBHelper.Users.ALL_FIELDS),
    COMMENTS("Comments", DBHelper.DATABASE_CREATE_COMMENTS),
    ACTIVITIES("Activities", DBHelper.DATABASE_CREATE_ACTIVITIES),
    RECORDINGS("Recordings", DBHelper.DATABASE_CREATE_RECORDINGS),
    SEARCHES("Searches", DBHelper.DATABASE_CREATE_SEARCHES),

    PLAYLIST("Playlist", DBHelper.DATABASE_CREATE_PLAYLIST),
    PLAYLIST_ITEMS("PlaylistItems", DBHelper.DATABASE_CREATE_PLAYLIST_ITEMS),

    COLLECTION_ITEMS("CollectionItems", DBHelper.DATABASE_CREATE_COLLECTION_ITEMS),
    COLLECTIONS("Collections", DBHelper.DATABASE_CREATE_COLLECTIONS),
    COLLECTION_PAGES("CollectionPages", DBHelper.DATABASE_CREATE_COLLECTION_PAGES),

    // views
    TRACK_VIEW("TrackView", DBHelper.DATABASE_CREATE_TRACK_VIEW),
    ACTIVITY_VIEW("ActivityView", DBHelper.DATABASE_CREATE_ACTIVITY_VIEW),
    ;


    public final String name;
    public final String createString;
    public final String id;
    public final String[] fields;
    public static final String TAG = DBHelper.TAG;

    Table(String name, String create, String... fields) {
        if (name == null || create == null) throw new NullPointerException();
        this.name = name;
        createString = create;
        id = this.name +"."+BaseColumns._ID;
        this.fields = fields;
    }

    public String allFields() {
        return this.name+".*";
    }

    public String field(String field) {
        return this.name+"."+field;
    }

    public static Table get(String name) {
        EnumSet<Table> tables = EnumSet.allOf(Table.class);
        for (Table table : tables) {
            if (table.name.equals(name)) return table;
        }
        return null;
    }

    public void drop(SQLiteDatabase db) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "dropping " + name);
        db.execSQL("DROP TABLE IF EXISTS "+name);
    }

    public void create(SQLiteDatabase db) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "creating " + name);
        if (!TextUtils.isEmpty(createString)) {
            db.execSQL(createString);
        }
    }

    public void recreate(SQLiteDatabase db) {
        drop(db);
        create(db);
    }

    public List<String> getColumnNames(SQLiteDatabase db) {
        return getColumnNames(db, name);
    }

    public static List<String> getColumnNames(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("pragma table_info (" + table + ")", null);
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

    public List<String> alterColumns(SQLiteDatabase db, String[] fromAppendCols, String[] toAppendCols) {
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
        db.execSQL(createString.replace("CREATE TABLE "+table, "CREATE TABLE "+tmpTable));

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
        if (fields == null) {
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
            db.execSQL(sql, bindArgs.toArray());
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return values.length;
    }

    @Override
    public String toString() {
        return name;
    }
}
