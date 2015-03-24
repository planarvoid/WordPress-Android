package com.soundcloud.android.testsupport;

import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.testsupport.fixtures.DatabaseFixtures;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.test.IntegrationTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import rx.schedulers.Schedulers;

import android.database.sqlite.SQLiteDatabase;

// use as a test base class when writing database integration tests
public abstract class StorageIntegrationTest extends IntegrationTest {

    @Rule public final HelperObjectsRule helpers = new HelperObjectsRule();

    private DatabaseFixtures helper;
    private DatabaseScheduler scheduler;
    private PropellerRx propellerRx;
    private DatabaseAssertions databaseAssertions;

    protected DatabaseFixtures testFixtures() {
        return helper;
    }

    protected DatabaseAssertions databaseAssertions() {
        return databaseAssertions;
    }

    protected DatabaseScheduler testScheduler() {
        return scheduler;
    }

    protected PropellerRx propellerRx() {
        return propellerRx;
    }

    @Override
    protected SQLiteDatabase provideDatabase() {
        return new DatabaseManager(Robolectric.application).getWritableDatabase();
    }

    public final class HelperObjectsRule extends ExternalResource {
        @Override
        protected void before() throws Throwable {
            helper = new DatabaseFixtures(database());
            databaseAssertions = new DatabaseAssertions(database());
            scheduler = new DatabaseScheduler(propeller(), Schedulers.immediate());
            propellerRx = new PropellerRx(propeller());
        }

    }
}
