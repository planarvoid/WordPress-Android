package com.soundcloud.android.utils;

import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import android.app.Application;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LeakCanaryWrapper {

    private final ApplicationProperties applicationProperties;
    private boolean installed = false;
    private RefWatcher refWatcher;

    @Inject
    public LeakCanaryWrapper(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public void install(Application application) {
        if (applicationProperties.isDevelopmentMode()) {
            refWatcher = LeakCanary.install(application);
            installed = true;
        }
    }

    public void watch(Object reference) {
        if (applicationProperties.isDevelopmentMode() && installed) {
            refWatcher.watch(reference);
        }
    }
}
