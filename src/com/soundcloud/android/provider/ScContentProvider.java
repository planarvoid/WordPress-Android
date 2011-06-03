package com.soundcloud.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.soundcloud.android.providers.ScContentProvider";
    public static final Pattern URL_PATTERN = Pattern.compile("^content://" + AUTHORITY + "/(\\w+)(?:/(\\d+))?$");

    private DatabaseHelper dbHelper;

    static class TableInfo {
        DatabaseHelper.Tables table;
        long id = -1;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        TableInfo info = getTableInfo(uri);
        if (info.id != -1) {
            selection = selection == null ? "_id=" + info.id : selection + " AND _id=" + info.id;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(info.table.tableName, columns, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final DatabaseHelper.Tables table = getTable(uri);
        long id = db.insert(table.tableName, null, values);
        if (id >= 0) {
            final Uri result = uri.buildUpon().appendPath(String.valueOf(id)).build();
            getContext().getContentResolver().notifyChange(result, null);
            return result;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        TableInfo table = getTableInfo(uri);
        if (table.id != -1) {
            where = "_id = ?";
            whereArgs = new String[]{String.valueOf(table.id)};
        }
        int count = db.delete(table.table.tableName, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(getTable(uri).name(), values, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    // to replace rows
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        String tblName = getTableInfo(uri).table.tableName;
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (db.replace(tblName, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    static DatabaseHelper.Tables getTable(Uri u) {
        return getTable(u.toString());
    }

    static DatabaseHelper.Tables getTable(String s) {
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches()) {
            return DatabaseHelper.Tables.get(m.group(1));
        }
        throw new IllegalArgumentException("unknown uri " + s);
    }

    static TableInfo getTableInfo(Uri uri) {
        return getTableInfo(uri.toString());
    }

    static TableInfo getTableInfo(String s) {
        TableInfo result = new TableInfo();
        result.table = getTable(s);
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches() && m.group(2) != null) {
            result.id = Long.parseLong(m.group(2));
        }
        return result;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }
}