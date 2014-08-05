package com.soundcloud.android.analytics;

import com.google.common.collect.Lists;
import com.localytics.android.Constants;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Module(addsTo = ApplicationModule.class, injects = SoundCloudApplication.class)
public class AnalyticsModule {

    private static final int EXPECTED_ANALYTICS_PROVIDERS = 4;

    @Provides
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public List<AnalyticsProvider> provideAnalyticsProviders(ApplicationProperties applicationProperties,
                                                             AnalyticsProperties analyticsProperties,
                                                             EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider,
                                                             PlayCountAnalyticsProvider playCountAnalyticsProvider,
                                                             LocalyticsAnalyticsProvider localyticsAnalyticsProvider,
                                                             @Nullable ComScoreAnalyticsProvider comScoreAnalyticsProvider) {
        Log.d(analyticsProperties.toString());
        Constants.IS_LOGGABLE = analyticsProperties.isAnalyticsAvailable() && applicationProperties.useVerboseLogging();
        // Unfortunately, both Localytics and ComScore are unmockable in tests and were crashing the tests during
        // initialization of AnalyticsEngine, so we do not register them unless we're running on a real device
        List<AnalyticsProvider> providers = Collections.emptyList();
        if (applicationProperties.isRunningOnDalvik()) {
            providers = new ArrayList<AnalyticsProvider>(EXPECTED_ANALYTICS_PROVIDERS);
            providers.add(eventLoggerAnalyticsProvider);
            providers.add(playCountAnalyticsProvider);
            providers.add(localyticsAnalyticsProvider);
            if (comScoreAnalyticsProvider != null) {
                providers.add(comScoreAnalyticsProvider);
            }
        }
        return providers;
    }

    @Provides
    @Nullable
    ComScoreAnalyticsProvider provideComScoreProvider(Context context) {
        // cf. https://github.com/soundcloud/SoundCloud-Android/issues/1811
        try {
            return new ComScoreAnalyticsProvider(context);
        } catch (Exception e) {
            SoundCloudApplication.handleSilentException("Error during Comscore library init", e);
            return null;
        }
    }

    @Provides
    @Named("tracking_db")
    PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }
}
