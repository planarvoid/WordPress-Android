package com.soundcloud.android.testsupport;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.SoundCloudApplication;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;

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
        shadows = {ShadowSupportMediaRouter.class},
        sdk = BuildConfig.ROBOELETRIC_SDK_VERSION)
public abstract class AndroidUnitTest {

    @Rule public TestRule injectMocksRule = (base, description) -> {
        MockitoAnnotations.initMocks(AndroidUnitTest.this);
        return base;
    };

    @Rule public TestRule rxJavaErrors = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            final Consumer<? super Throwable> previousErrorHandler = RxJavaPlugins.getErrorHandler();
            RxJavaPlugins.setErrorHandler(e -> {
                Thread.currentThread().setUncaughtExceptionHandler((t, f) -> {
                    Thread.currentThread().setUncaughtExceptionHandler(null);
                    throw new RuntimeException(f);
                });
                SoundCloudApplication.handleThrowableInDebug(e);
            });
            base.evaluate();
            RxJavaPlugins.setErrorHandler(previousErrorHandler);
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
        return context().getSharedPreferences("Test", Context.MODE_PRIVATE);
    }
}
