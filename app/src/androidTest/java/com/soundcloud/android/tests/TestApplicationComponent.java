package com.soundcloud.android.tests;

import com.soundcloud.android.ApplicationComponent;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Must be kept in sync with the ApplicationComponent class.
 */
@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class,
        StorageModule.class,
        AnalyticsModule.class
})
public interface TestApplicationComponent extends ApplicationComponent {
    void inject(SoundCloudTestApplication application);
}
