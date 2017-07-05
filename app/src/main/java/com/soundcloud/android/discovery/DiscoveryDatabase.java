package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.RxJava;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.QueryObservable;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import io.reactivex.Observable;
import io.reactivex.Single;
import rx.Scheduler;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DiscoveryDatabase {

    private final BriteDatabase briteDatabase;

    @Inject
    DiscoveryDatabase(DiscoveryDatabaseOpenHelper discoveryDatabaseOpenHelper, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        briteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(discoveryDatabaseOpenHelper, scheduler);
    }

    void cleanUp() throws SQLException {
        runInTransaction(() -> {
            final SQLiteDatabase writableDatabase = briteDatabase.getWritableDatabase();
            writableDatabase.execSQL(DbModel.SelectionItem.DELETEALL);
            writableDatabase.execSQL(DbModel.SingleContentSelectionCard.DELETEALL);
            writableDatabase.execSQL(DbModel.MultipleContentSelectionCard.DELETEALL);
            writableDatabase.execSQL(DbModel.DiscoveryCard.DELETEALL);
            writableDatabase.execSQL(DbModel.SystemPlaylistsTracks.DELETEALL);
            writableDatabase.execSQL(DbModel.SystemPlaylist.DELETEALL);
        });
    }

    SQLiteDatabase writableDatabase() {
        return briteDatabase.getWritableDatabase();
    }

    long insert(String table, SQLiteStatement statement) throws SQLException {
        return briteDatabase.executeInsert(table, statement);
    }

    long updateOrDelete(String table, SQLiteStatement statement) throws SQLException {
        return briteDatabase.executeUpdateDelete(table, statement);
    }

    void runInTransaction(Runnable runnable) {
        final BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            runnable.run();
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    void batchInsert(String table, List<SQLiteStatement> statements) throws SQLException {
        for (SQLiteStatement statement : statements) {
            briteDatabase.executeInsert(table, statement);
        }
    }

    <T> Observable<List<T>> observeList(final RowMapper<T> mapper, final String tableName, final String query, final String... args) {
        final QueryObservable discoveryCardsQuery = briteDatabase.createQuery(tableName, query, args);
        return RxJava.toV2Observable(discoveryCardsQuery.mapToList(mapper::map));
    }

    <T> Single<List<T>> selectList(SqlDelightStatement sqlDelightStatement, RowMapper<T> selectionItemMapper) {
        return Single.fromCallable(() -> {
            final List<T> contactList = new ArrayList<>();
            final Cursor cursor = briteDatabase.query(sqlDelightStatement.statement, sqlDelightStatement.args);
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    contactList.add(selectionItemMapper.map(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            return contactList;
        });
    }
}
