package com.soundcloud.android.robolectric;

import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.test.IntegrationTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import rx.schedulers.Schedulers;

import android.database.sqlite.SQLiteDatabase;

// use as a test base class when writing database integration tests
public abstract class StorageIntegrationTest extends IntegrationTest {

    @Rule public final HelperObjectsRule helpers = new HelperObjectsRule();

    private DatabaseHelper helper;
    private DatabaseScheduler scheduler;

    protected DatabaseHelper testHelper() {
        return helper;
    }

    protected DatabaseScheduler testScheduler() {
        return scheduler;
    }

    @Override
    protected SQLiteDatabase provideDatabase() {
        return new DatabaseManager(Robolectric.application).getWritableDatabase();
    }

    public final class HelperObjectsRule extends ExternalResource {
        @Override
        protected void before() throws Throwable {
            helper = new DatabaseHelper(database());
            scheduler = new DatabaseScheduler(propeller(), Schedulers.immediate());
        }

    }
}
