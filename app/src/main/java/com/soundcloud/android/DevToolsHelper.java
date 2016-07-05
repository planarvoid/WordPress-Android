package com.soundcloud.android;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.properties.ApplicationProperties;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Named;

public class DevToolsHelper {
    private final ApplicationProperties applicationProperties;
    private final OkHttpClient defaultClient;

    @Inject
    DevToolsHelper(ApplicationProperties applicationProperties,
                   @Named(ApiModule.API_HTTP_CLIENT) OkHttpClient defaultClient) {
        this.applicationProperties = applicationProperties;
        this.defaultClient = defaultClient;
    }

    void initialize(SoundCloudApplication application) {
        if (applicationProperties.isDebugBuild()) {
            leakCanary(application);
            stetho(application);
        }
    }

    private void leakCanary(SoundCloudApplication application) {
        LeakCanary.install(application);
    }

    // Open chrome://inspect/ (in Chrome).
    private void stetho(SoundCloudApplication application) {
        Stetho.initializeWithDefaults(application);
        defaultClient.networkInterceptors().add(new StethoInterceptor());
    }

}
