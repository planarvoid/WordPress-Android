package com.soundcloud.android.analytics;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class, injects = SoundCloudApplication.class)
public class AnalyticsModule {

    @Provides
    public AnalyticsProviderFactory provideAnalyticsProviderFactory(SharedPreferences sharedPreferences,
                                                                    ApplicationProperties applicationProperties,
                                                                    AnalyticsProperties analyticsProperties,
                                                                    EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider,
                                                                    PlayCountAnalyticsProvider playCountAnalyticsProvider,
                                                                    LocalyticsAnalyticsProvider localyticsAnalyticsProvider,
                                                                    @Nullable ComScoreAnalyticsProvider comScoreAnalyticsProvider,
                                                                    PromotedAnalyticsProvider promotedAnalyticsProvider) {
        return new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences,
                eventLoggerAnalyticsProvider, playCountAnalyticsProvider,localyticsAnalyticsProvider,
                promotedAnalyticsProvider, comScoreAnalyticsProvider);
    }

    @Provides
    @Nullable
    ComScoreAnalyticsProvider provideComScoreProvider(Context context) {
        // cf. https://github.com/soundcloud/SoundCloud-Android/issues/1811
        try {
            return new ComScoreAnalyticsProvider(context);
        } catch (Exception e) {
            ErrorUtils.handleSilentException("Error during Comscore library init", e);
            return null;
        }
    }

    @Provides
    @Named("tracking_db")
    PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }
}
