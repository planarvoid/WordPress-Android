package com.soundcloud.android.testsupport;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.testsupport.fixtures.DatabaseFixtures;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.test.IntegrationTest;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(packageName = "com.soundcloud.android",
        constants = BuildConfig.class,
        application = ApplicationStub.class,
        sdk = 21)
public abstract class StorageIntegrationTest extends IntegrationTest {

    @Rule public final HelperObjectsRule helpers = new HelperObjectsRule();
    @Rule public TestRule injectMocksRule = new InjectMocksRule();

    private DatabaseFixtures helper;
    private PropellerRx propellerRx;
    private DatabaseAssertions databaseAssertions;

    protected DatabaseFixtures testFixtures() {
        return helper;
    }

    protected DatabaseAssertions databaseAssertions() {
        return databaseAssertions;
    }

    protected PropellerRx propellerRx() {
        return propellerRx;
    }

    protected static SharedPreferences sharedPreferences() {
        return new RoboSharedPreferences(new HashMap<String, Map<String, Object>>(), "Test", Context.MODE_PRIVATE);
    }

    @Override
    protected SQLiteDatabase provideDatabase() {
        final ApplicationProperties applicationProperties = provideApplicationProperties();
        return new DatabaseManager(RuntimeEnvironment.application, applicationProperties).getWritableDatabase();
    }

    private ApplicationProperties provideApplicationProperties() {
        return new ApplicationProperties(RuntimeEnvironment.application.getResources());
    }

    public final class HelperObjectsRule extends ExternalResource {
        @Override
        protected void before() throws Throwable {
            helper = new DatabaseFixtures(database());
            databaseAssertions = new DatabaseAssertions(database());
            propellerRx = new PropellerRx(propeller());
        }

    }

    public final class InjectMocksRule implements TestRule {
        @Override
        public Statement apply(Statement base, Description description) {
            MockitoAnnotations.initMocks(StorageIntegrationTest.this);
            return base;
        }
    }
}

