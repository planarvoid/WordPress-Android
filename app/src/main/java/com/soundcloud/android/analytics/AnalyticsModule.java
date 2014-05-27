package com.soundcloud.android.analytics;

import com.google.common.collect.Lists;
import com.localytics.android.Constants;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLogger;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerParamsBuilder;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.Log;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import android.content.Context;

import java.util.Collections;
import java.util.List;

@Module(addsTo = ApplicationModule.class, injects = SoundCloudApplication.class)
public class AnalyticsModule {

    @Provides
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public List<AnalyticsProvider> provideAnalyticsProviders(Context context,
                                                             ApplicationProperties applicationProperties,
                                                             AnalyticsProperties analyticsProperties,
                                                             AccountOperations accountOperations,
                                                             EventLogger eventLogger,
                                                             EventLoggerParamsBuilder eventLoggerParamsBuilder) {
        Log.d(analyticsProperties.toString());
        Constants.IS_LOGGABLE = analyticsProperties.isAnalyticsAvailable() && applicationProperties.useVerboseLogging();
        // Unfortunately, both Localytics and ComScore are unmockable in tests and were crashing the tests during
        // initialiation of AnalyticsEngine, so we do not register them unless we're running on a real device
        if (applicationProperties.isRunningOnDalvik()) {
            return Lists.newArrayList(
                    new LocalyticsAnalyticsProvider(context, analyticsProperties, accountOperations.getLoggedInUserId()),
                    new EventLoggerAnalyticsProvider(eventLogger, eventLoggerParamsBuilder),
                    new ComScoreAnalyticsProvider(context));
        } else {
            return Collections.emptyList();
        }
    }
}
