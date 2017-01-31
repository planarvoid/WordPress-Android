package com.soundcloud.android.tests;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Application subclass used in UI tests. */
public class SoundCloudTestApplication extends SoundCloudApplication {
    /** Used in tests to wait for {@link #onCreate()} to complete. */
    private final CountDownLatch onCreateLatch = new CountDownLatch(1);

    @Override
    public void onCreate() {
        super.onCreate();
        onCreateLatch.countDown();
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
            return ((SoundCloudTestApplication) c.getApplicationContext());
        } else {
            throw new RuntimeException("can't obtain app from context");
        }
    }
}
