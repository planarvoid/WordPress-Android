package com.soundcloud.android.storage;

import com.soundcloud.android.ApplicationModule;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.QueryObservable;
import com.squareup.sqlbrite2.SqlBrite;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class SqlBriteDatabase {
    protected final BriteDatabase briteDatabase;

    public SqlBriteDatabase(SQLiteOpenHelper databaseOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        briteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(databaseOpenHelper, scheduler);
    }

    public SQLiteDatabase writableDatabase() {
        return briteDatabase.getWritableDatabase();
    }

    public SQLiteDatabase readableDatabase() {
        return briteDatabase.getWritableDatabase();
    }

    public long insert(String table, SQLiteStatement statement) throws SQLException {
        return briteDatabase.executeInsert(table, statement);
    }

    public long updateOrDelete(String table, SQLiteStatement statement) throws SQLException {
        return briteDatabase.executeUpdateDelete(table, statement);
    }

    public int clear(String table) {
        return briteDatabase.delete(table, null, null);
    }

    public void runInTransaction(Runnable runnable) {
        final BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            runnable.run();
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public void batchInsert(String table, List<SQLiteStatement> statements) throws SQLException {
        for (SQLiteStatement statement : statements) {
            briteDatabase.executeInsert(table, statement);
        }
    }

    public <T> Observable<List<T>> executeObservableQuery(final RowMapper<T> mapper, final String tableName, final String query, final String... args) {
        final QueryObservable discoveryCardsQuery = briteDatabase.createQuery(tableName, query, args);
        return discoveryCardsQuery.mapToList(mapper::map);
    }

    public <T> Single<List<T>> executeAsyncQuery(SqlDelightStatement sqlDelightStatement, RowMapper<T> selectionItemMapper) {
        return Single.fromCallable(() -> executeQuery(sqlDelightStatement, selectionItemMapper));
    }

    public <T> List<T> executeQuery(SqlDelightStatement sqlDelightStatement, RowMapper<T> itemMapper) {
        final List<T> resultList = new ArrayList<>();
        Cursor cursor = briteDatabase.query(sqlDelightStatement.statement, sqlDelightStatement.args);
        try {
            while (cursor.moveToNext()) {
                resultList.add(itemMapper.map(cursor));
            }
        } finally {
            cursor.close();
        }
        return resultList;
    }
}
