package com.soundcloud.android.di;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.di.testimplementations.NoopGooglePlayServicesWrapper;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;

public class TestApplicationModule extends ApplicationModule {
    public TestApplicationModule(SoundCloudTestApplication application) {
        super(application);
    }

    @Override
    public GooglePlayServicesWrapper provideGooglePlayServicesWrapper() {
        return new NoopGooglePlayServicesWrapper();
    }
}
