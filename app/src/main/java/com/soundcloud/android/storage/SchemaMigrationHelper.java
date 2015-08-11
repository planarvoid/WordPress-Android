package com.soundcloud.android.storage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// contains methods pulled out of the old Table enum that deal with schema migrations
public final class SchemaMigrationHelper {
    private static final String TAG = SchemaMigrationHelper.class.getSimpleName();

    private SchemaMigrationHelper() {
        // no instances
    }

    public static void drop(Table table, SQLiteDatabase db) {
        if (Log.isLoggable(Table.TAG, Log.DEBUG)) {
            Log.d(Table.TAG, "dropping " + table.name());
        }
        if (table.view){
            db.execSQL("DROP VIEW IF EXISTS " + table.name());
        } else {
            dropTable(table.name(), db);
        }

    }

    public static void dropTable(String tableName, SQLiteDatabase db) {
        if (Log.isLoggable(Table.TAG, Log.DEBUG)) {
            Log.d(Table.TAG, "dropping " + tableName);
        }
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
    }

    public static void create(Table table, SQLiteDatabase db) {
        if (!TextUtils.isEmpty(table.createString)) {
            if (Log.isLoggable(Table.TAG, Log.DEBUG)) {
                Log.d(Table.TAG, "creating " + (table.view ? "view" : "table") + " " + table.name());
            }
            db.execSQL(table.createString);
        } else if (Log.isLoggable(Table.TAG, Log.DEBUG)) {
            Log.d(Table.TAG, "NOT creating " + table.name());
        }
    }

    public static void recreate(Table table, SQLiteDatabase db) {
        drop(table, db);
        create(table, db);
    }

    public static List<String> alterColumns(Table table, SQLiteDatabase db) {
        return alterColumns(db, table.name(), table.createString, new String[0], new String[0]);
    }

    public static List<String> alterColumns(String tableName, String createString, SQLiteDatabase db) {
        return alterColumns(db, tableName, createString, new String[0], new String[0]);
    }

    @Deprecated // should be a private method
    static List<String> alterColumns(SQLiteDatabase db,
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

    public static List<String> getColumnNames(Table table, SQLiteDatabase db) {
        return getColumnNames(db, table.name());
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

    public static void migrate(SQLiteDatabase db, String toTable, List<String> toColumns,
                               String fromTable, List<String> fromColumns) {
        final String toCols = TextUtils.join(",", toColumns);
        final String fromCols = TextUtils.join(",", fromColumns);
        final String sql = String.format(Locale.ENGLISH, "INSERT INTO %s (%s) SELECT %s from %s", toTable, toCols, fromCols, fromTable);

        Log.d(TAG, "migrating " + sql);
        db.execSQL(sql);
    }
}
