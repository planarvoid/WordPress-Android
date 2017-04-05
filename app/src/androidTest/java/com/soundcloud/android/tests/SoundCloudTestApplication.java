package com.soundcloud.android.tests;

import com.soundcloud.android.ApplicationComponent;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.di.TestAnalyticsModule;
import com.soundcloud.android.di.TestApiModule;
import com.soundcloud.android.di.TestApplicationModule;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Application subclass used in UI tests.
 */
public class SoundCloudTestApplication extends SoundCloudApplication {
    /**
     * Used in tests to wait for {@link #onCreate()} to complete.
     */
    private final CountDownLatch onCreateLatch = new CountDownLatch(1);
    private TestApplicationComponent testApplicationComponent;

    @Inject FeatureFlags featureFlags;
    @Inject EventBus eventBus;
    @Inject AccountOperations accountOperations;

    @Override
    public void onCreate() {
        beforeOnCreate();
        super.onCreate();
        onCreateLatch.countDown();
        testApplicationComponent.inject(this);
    }

    private void beforeOnCreate() {
    }

    /**
     * Waits up to the specified timeout for {@link #onCreate()} to run. If onCreate has already run,
     * returns immediately.
     *
     * @throws IllegalStateException if onCreate does not run within the timeout
     */
    public void awaitOnCreate(long timeout, TimeUnit unit) throws InterruptedException {
        if (!onCreateLatch.await(timeout, unit)) {
            throw new IllegalStateException("onCreate not called after " + timeout + " " + unit);
        }
    }

    public static SoundCloudTestApplication fromContext(Context c) {
        if (c.getApplicationContext() instanceof SoundCloudTestApplication) {
            return (SoundCloudTestApplication) c.getApplicationContext();
        } else {
            throw new RuntimeException("can't obtain app from context");
        }
    }

    @Override
    protected ApplicationComponent buildApplicationComponent() {
        if (testApplicationComponent != null) {
            throw new IllegalStateException("ApplicationComponent already built.");
        }

        testApplicationComponent = DaggerTestApplicationComponent.builder()
                                                                 .applicationModule(new TestApplicationModule(this))
                                                                 .apiModule(new TestApiModule())
                                                                 .analyticsModule(new TestAnalyticsModule())
                                                                 .build();
        return testApplicationComponent;
    }

    public FeatureFlags getFeatureFlags() {
        return featureFlags;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public AccountOperations getAccountOperations() {
        return accountOperations;
    }
}
