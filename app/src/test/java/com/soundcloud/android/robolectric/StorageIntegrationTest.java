package com.soundcloud.android.robolectric;

import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.xtremelabs.robolectric.Robolectric;
import rx.schedulers.Schedulers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

// use as a test base class when writing database integration tests
public abstract class StorageIntegrationTest {

    private SQLiteDatabase database = new DatabaseManager(Robolectric.application).getWritableDatabase();
    private PropellerDatabase propeller = new PropellerDatabase(database);
    private DatabaseHelper helper = new DatabaseHelper(database);
    private DatabaseScheduler scheduler = new DatabaseScheduler(propeller, Schedulers.immediate());

    protected DatabaseHelper testHelper() {
        return helper;
    }

    protected DatabaseScheduler testScheduler() {
        return scheduler;
    }

    protected SQLiteDatabase database() {
        return database;
    }

    protected PropellerDatabase propeller() {
        return propeller;
    }

    protected boolean exists(String table, Where conditions) {
        return count(table, conditions) > 0;
    }

    protected boolean exists(String table) {
        return exists(table, filter());
    }

    protected int count(String table, Where conditions) {
        final Query query = Query.count(table).where((WhereBuilder) conditions);
        final Cursor cursor = database.rawQuery(query.build(), query.getArguments());
        if (cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    protected int count(String table) {
        return count(table, filter());
    }

    protected WhereBuilder filter() {
        return new WhereBuilder();
    }
}
