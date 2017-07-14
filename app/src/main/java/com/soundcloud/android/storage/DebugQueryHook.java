package com.soundcloud.android.storage;

import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.DatabaseHook;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Table;

import android.content.ContentValues;

class DebugQueryHook implements DatabaseHook {

    static final String TAG = "QueryDebug";

    private static final int MAX_DEBUG_STRING_LENGTH = 200;
    private static final String UPSERT = "UPSERT";
    private static final String BULK_UPSERT = "BULK UPSERT";
    private static final String DELETE = "DELETE";
    private static final String TRUNCATE = "TRUNCATE";
    private static final String UPDATE = "UPDATE";
    private static final String INSERT = "INSERT";
    private static final String BULK_INSERT = "BULK INSERT";

    private final SlowQueryReporter slowQueryReporter;
    private boolean logQueries;

    DebugQueryHook(SlowQueryReporter slowQueryReporter, boolean logQueries) {
        this.slowQueryReporter = slowQueryReporter;
        this.logQueries = logQueries;
    }

    @Override
    public void onQueryStarted(Query query) {
        started(query.build());
    }

    @Override
    public void onQueryFinished(Query query, long duration) {
        finished(query.build(), duration);
    }

    @Override
    public void onQueryStarted(String query) {
        started(query);
    }

    @Override
    public void onQueryFinished(String query, long duration) {
        finished(query, duration);
    }

    @Override
    public void onInsertStarted(String table, ContentValues contentValues, int i) {
        started(format(INSERT, table, contentValues));
    }

    @Override
    public void onInsertFinished(String table,
                                 ContentValues contentValues,
                                 int i,
                                 InsertResult insertResult,
                                 long duration) {
        finished(format(INSERT, table, contentValues), duration);
    }

    @Override
    public void onBulkInsertStarted(String table, BulkInsertValues bulkInsertValues) {
        started(format(BULK_INSERT, table, bulkInsertValues));
    }

    @Override
    public void onBulkInsertFinished(String table,
                                     BulkInsertValues bulkInsertValues,
                                     TxnResult txnResult,
                                     long duration) {
        finished(format(BULK_INSERT, table, bulkInsertValues), duration);
    }


    @Override
    public void onUpsert(Table table, ContentValues contentValues) {
        started(format(UPSERT, table.name(), contentValues));
    }

    @Override
    public void onUpsertFinished(Table table, ContentValues contentValues, ChangeResult changeResult, long duration) {
        finished(format(UPSERT, table.name(), contentValues), duration);
    }

    @Override
    public void onBulkUpsertStarted(Table table, Iterable<ContentValues> iterable) {
        started(format(BULK_UPSERT, table.name(), iterable));
    }

    @Override
    public void onBulkUpsertFinished(Table table,
                                     Iterable<ContentValues> iterable,
                                     TxnResult txnResult,
                                     long duration) {

        finished(format(BULK_UPSERT, table.name(), iterable), duration);
    }

    @Override
    public void onUpdate(String table, ContentValues contentValues, Where where) {
        started(format(UPDATE, table, contentValues, where));
    }

    @Override
    public void onUpdateFinished(String table, ContentValues contentValues, Where where, long duration) {
        finished(format(UPDATE, table, contentValues, where), duration);
    }

    @Override
    public void onDelete(String table) {
        started(format(DELETE, table));
    }

    @Override
    public void onDeleteFinished(String table, long duration) {
        finished(format(DELETE, table), duration);
    }

    @Override
    public void onDelete(String table, Where where) {
        started(format(DELETE, table, where));
    }

    @Override
    public void onDeleteFinished(String table, Where where, long duration) {
        finished(format(DELETE, table, where), duration);
    }

    @Override
    public void onTruncate(String table) {
        started(format(TRUNCATE, table));
    }

    @Override
    public void onTruncateFinished(String table, long duration) {
        finished(format(TRUNCATE, table), duration);
    }

    private static String format(String operationName, String table) {
        return operationName + " " + table;
    }

    private static String format(String operationName, String table, Iterable<ContentValues> contentValues) {
        return operationName + " " + table + " " + Iterables.toString(contentValues);
    }

    private static String format(String operationName, String table, Where where) {
        return operationName + " " + table + " " + where.getSelection();
    }

    private static String format(String operationName, String table, ContentValues contentValues, Where where) {
        return operationName + " " + table + " " + contentValues + " " + where.getSelection();
    }

    private static String format(String operationName, String table, ContentValues contentValues) {
        return operationName + " " + table + " " + contentValues;
    }

    private static String format(String operationName, String table, BulkInsertValues bulkInsertValues) {
        return operationName + " " + table + " " + bulkInsertValues;
    }

    private void started(String message) {
        if (logQueries) {
            Log.d(TAG, "start : " + limit(message));
        }
    }

    private void finished(String message, long duration) {
        if (logQueries) {
            Log.d(TAG, "finish (" + duration + "ms) : " + limit(message));
        }
        slowQueryReporter.reportIfSlow(new DebugDatabaseStat(message, duration));
    }

    static String limit(String query) {
        return query.length() <= MAX_DEBUG_STRING_LENGTH ? query : query.substring(0, MAX_DEBUG_STRING_LENGTH);
    }
}
