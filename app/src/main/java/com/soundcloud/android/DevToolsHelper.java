package com.soundcloud.android;

import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.leakcanary.LeakCanary;

import javax.inject.Inject;

public class DevToolsHelper {
    private final ApplicationProperties applicationProperties;

    @Inject
    DevToolsHelper(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    private void leakCanary(SoundCloudApplication application) {
        LeakCanary.install(application);
    }

    void initialize(SoundCloudApplication application) {
        if (applicationProperties.isDevelopmentMode()) {
            leakCanary(application);
        }
    }
}
