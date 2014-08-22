package com.soundcloud.android.analytics;

import com.localytics.android.Constants;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.Log;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalyticsProviderFactory {

    private static final int EXPECTED_PROVIDER_COUNT = 5;

    private final SharedPreferences sharedPreferences;
    private final ApplicationProperties applicationProperties;
    private final AnalyticsProperties analyticsProperties;
    private final EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;
    private final PlayCountAnalyticsProvider playCountAnalyticsProvider;
    private final LocalyticsAnalyticsProvider localyticsAnalyticsProvider;
    private final PromotedAnalyticsProvider promotedAnalyticsProvider;

    @Nullable private final ComScoreAnalyticsProvider comScoreAnalyticsProvider;

    public AnalyticsProviderFactory(AnalyticsProperties analyticsProperties,
                                    ApplicationProperties applicationProperties,
                                    SharedPreferences sharedPreferences,
                                    EventLoggerAnalyticsProvider eventLoggerProvider,
                                    PlayCountAnalyticsProvider playCountProvider,
                                    LocalyticsAnalyticsProvider localyticsProvider,
                                    PromotedAnalyticsProvider promotedProvider,
                                    @Nullable ComScoreAnalyticsProvider comScoreProvider) {
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
        this.analyticsProperties = analyticsProperties;
        this.eventLoggerAnalyticsProvider = eventLoggerProvider;
        this.playCountAnalyticsProvider = playCountProvider;
        this.localyticsAnalyticsProvider = localyticsProvider;
        this.comScoreAnalyticsProvider = comScoreProvider;
        this.promotedAnalyticsProvider = promotedProvider;
    }

    public List<AnalyticsProvider> getProviders() {
        initLogging();
        if (!analyticsProperties.isAnalyticsAvailable()) {
            return Collections.emptyList();
        }

        List<AnalyticsProvider> providers = getBaseProviders();
        if (sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)) {
            getOptInProviders(providers);
        }
        return providers;
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private void initLogging() {
        Log.d(analyticsProperties.toString());
        Constants.IS_LOGGABLE = analyticsProperties.isAnalyticsAvailable() && applicationProperties.useVerboseLogging();
    }

    private List<AnalyticsProvider> getBaseProviders() {
        List<AnalyticsProvider> providers = new ArrayList<AnalyticsProvider>(EXPECTED_PROVIDER_COUNT);
        providers.add(eventLoggerAnalyticsProvider);
        providers.add(playCountAnalyticsProvider);
        providers.add(promotedAnalyticsProvider);
        return providers;
    }

    private List<AnalyticsProvider> getOptInProviders(List<AnalyticsProvider> providers) {
        providers.add(localyticsAnalyticsProvider);
        if (comScoreAnalyticsProvider != null) {
            providers.add(comScoreAnalyticsProvider);
        }
        return providers;
    }

}
