package com.soundcloud.android.robolectric;

import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.QueryResult;
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

    protected int rowCount(String table) {
        Cursor cursor = database.rawQuery("select count(*) from " + table, null);
        if (cursor.moveToNext()) {
            return cursor.getInt(0);
        } else {
            return -1;
        }
    }

    protected boolean exists(Query query) {
        return count(query) > 0;
    }

    protected boolean exists(String table) {
        return count(table) > 0;
    }

    protected int count(Query query) {
        final QueryResult result = propeller.query(query.count());
        for (CursorReader cursor : result) {
            return cursor.getInt("count(*)");
        }
        return -1;
    }

    protected int count(String table) {
        final QueryResult result = propeller.query(Query.from(table).count());
        for (CursorReader cursor : result) {
            return cursor.getInt("count(*)");
        }
        return -1;
    }
}
