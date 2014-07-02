package com.soundcloud.android.robolectric;

import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.xtremelabs.robolectric.Robolectric;
import rx.schedulers.Schedulers;

import android.database.sqlite.SQLiteDatabase;

// use as a test base class when writing database integration tests
public abstract class StorageIntegrationTest {

    private SQLiteDatabase sqliteDatabase = new DatabaseManager(Robolectric.application).getWritableDatabase();
    private PropellerDatabase database = new PropellerDatabase(sqliteDatabase);
    private DatabaseHelper helper = new DatabaseHelper(sqliteDatabase);
    private DatabaseScheduler scheduler = new DatabaseScheduler(database, Schedulers.immediate());

    protected DatabaseHelper testHelper() {
        return helper;
    }

    protected DatabaseScheduler testScheduler() {
        return scheduler;
    }
}
