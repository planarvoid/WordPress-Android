package com.soundcloud.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum Table {
    TRACKS("Tracks", DBHelper.DATABASE_CREATE_TRACKS, DBHelper.Tracks.ALL_FIELDS),
    TRACK_PLAYS("TrackPlays", DBHelper.DATABASE_CREATE_TRACK_PLAYS),
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
        Log.d(DBHelper.TAG, "dropping " + name);
        db.execSQL("DROP TABLE IF EXISTS "+name);
    }

    public void create(SQLiteDatabase db) {
        Log.d(DBHelper.TAG, "creating " + name);
        db.execSQL(createString);
    }

    public void recreate(SQLiteDatabase db) {
        drop(db);
        create(db);
    }

    public List<String> getColumnNames(SQLiteDatabase db, String table) {
        String _table = table == null ? name : table;
        Cursor cursor = db.rawQuery("pragma table_info (" + _table + ")", null);
        List<String> cols = new ArrayList<String>();
        while (cursor != null && cursor.moveToNext()) {
            cols.add(cursor.getString(1));
        }
        if (cursor != null) cursor.close();
        return cols;
    }

    public String alterColumns(SQLiteDatabase db,
                               String[] fromAppendCols,
                               String[] toAppendCols) {
        String tmpTable = "bck_"+name;
        db.execSQL("DROP TABLE IF EXISTS "+tmpTable);
        db.execSQL(createString.replace("CREATE TABLE "+name, "CREATE TABLE "+tmpTable));

        List<String> columns = getColumnNames(db, tmpTable);
        columns.retainAll(getColumnNames(db, null));
        String cols = TextUtils.join(",", columns);

        String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", toAppendCols) : cols;

        String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                + TextUtils.join(",", fromAppendCols) : cols;

        db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from %s", tmpTable, toCols, fromCols, name));

        recreate(db);

        db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from %s", name, cols, cols, tmpTable));
        db.execSQL("DROP table "+tmpTable);
        return cols;
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
            db.execSQL(sb.toString(), bindArgs.toArray());
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
