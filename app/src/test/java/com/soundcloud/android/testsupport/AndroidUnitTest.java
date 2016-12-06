package com.soundcloud.android.testsupport;

import com.soundcloud.android.BuildConfig;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboSharedPreferences;
import org.robolectric.shadows.ShadowApplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;

import java.util.HashMap;

/**
 * Base class for unit tests that have hard dependencies on the Android platform.
 * This is true for test subjects that are either derived from framework components
 * (such as Fragments or Adapters) or use framework classes internally that can't
 * be mocked out (such as Bundles or Intents)
 * <p>
 * For anything else, use an ordinary JUnit test with the {@link org.mockito.runners.MockitoJUnitRunner}
 * and mock out collaborators that come from the framework.
 */
@RunWith(RobolectricTestRunner.class)
@Config(packageName = "com.soundcloud.android",
        constants = BuildConfig.class,
        application = ApplicationStub.class,
        shadows={ShadowSupportMediaRouter.class},
        sdk = 22)
public abstract class AndroidUnitTest {

    @Rule public TestRule injectMocksRule = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            MockitoAnnotations.initMocks(AndroidUnitTest.this);
            return base;
        }
    };

    protected static Intent getNextStartedService() {
        return ShadowApplication.getInstance().getNextStartedService();
    }

    protected static AttributeSet attributeSet() {
        return Robolectric.buildAttributeSet().build();
    }

    protected static Context context() {
        return RuntimeEnvironment.application.getBaseContext();
    }

    protected static AppCompatActivity activity() {
        return Robolectric.buildActivity(AppCompatActivity.class)
                          .create()
                          .start()
                          .resume()
                          .get();
    }

    protected static Resources resources() {
        return context().getResources();
    }

    protected static SharedPreferences sharedPreferences() {
        return new RoboSharedPreferences(new HashMap<>(), "Test", Context.MODE_PRIVATE);
    }
}
