package com.soundcloud.android.gcm;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {GcmRegistrationService.class, GcmMessageReceiver.class},
        includes = AnalyticsModule.class)
public class GcmModule {
}
