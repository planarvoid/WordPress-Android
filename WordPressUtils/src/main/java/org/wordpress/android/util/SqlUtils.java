package org.wordpress.android.util;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

public class SqlUtils {
    private SqlUtils() {
        throw new AssertionError();
    }

    /*
     * SQLite doesn't have a boolean datatype, so booleans are stored as 0=false, 1=true
     */
    public static long boolToSql(boolean value) {
        return (value ? 1 : 0);
    }
    public static boolean sqlToBool(int value) {
        return (value != 0);
    }

    public static void closeStatement(SQLiteStatement stmt) {
        if (stmt != null) {
            stmt.close();
        }
    }

    public static void closeCursor(Cursor c) {
        if (c != null && !c.isClosed()) {
            c.close();
        }
    }

    /*
     * wrapper for DatabaseUtils.longForQuery() which returns 0 if query returns no rows
     */
    public static long longForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try {
            return DatabaseUtils.longForQuery(db, query, selectionArgs);
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    public static int intForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        long value = longForQuery(db, query, selectionArgs);
        return (int)value;
    }

    public static boolean boolForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        long value = longForQuery(db, query, selectionArgs);
        return sqlToBool((int) value);
    }

    /*
     * wrapper for DatabaseUtils.stringForQuery(), returns "" if query returns no rows
     */
    public static String stringForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try {
            return DatabaseUtils.stringForQuery(db, query, selectionArgs);
        } catch (SQLiteDoneException e) {
            return "";
        }
    }

    /*
     * returns the number of rows in the passed table
     */
    public static long getRowCount(SQLiteDatabase db, String tableName) {
        return DatabaseUtils.queryNumEntries(db, tableName);
    }

    /*
     * removes all rows from the passed table
     */
    public static void deleteAllRowsInTable(SQLiteDatabase db, String tableName) {
        db.delete(tableName, null, null);
    }

    /*
     * drop all tables from the passed SQLiteDatabase - make sure to pass a
     * writable database
     */
    public static boolean dropAllTables(SQLiteDatabase db) throws SQLiteException {
        if (db == null) {
            return false;
        }

        if (db.isReadOnly()) {
            throw new SQLiteException("can't drop tables from a read-only database");
        }

        List<String> tableNames = new ArrayList<String>();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (cursor.moveToFirst()) {
            do {
                String tableName = cursor.getString(0);
                if (!tableName.equals("android_metadata") && !tableName.equals("sqlite_sequence")) {
                    tableNames.add(tableName);
                }
            } while (cursor.moveToNext());
        }

        db.beginTransaction();
        try {
            for (String tableName: tableNames) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
}
