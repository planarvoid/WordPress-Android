package com.soundcloud.android.analytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.crashlytics.CrashlyticsAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.utils.Log;
import io.fabric.sdk.android.Fabric;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import javax.inject.Inject;
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
    private final AdjustAnalyticsProvider adjustAnalyticsProvider;
    private final CrashlyticsAnalyticsProvider crashlyticsAnalyticsProvider;

    @Nullable private final ComScoreAnalyticsProvider comScoreAnalyticsProvider;


    @Inject
    public AnalyticsProviderFactory(AnalyticsProperties analyticsProperties,
                                    ApplicationProperties applicationProperties,
                                    SharedPreferences sharedPreferences,
                                    EventLoggerAnalyticsProvider eventLoggerProvider,
                                    PlayCountAnalyticsProvider playCountProvider,
                                    LocalyticsAnalyticsProvider localyticsProvider,
                                    PromotedAnalyticsProvider promotedProvider,
                                    AdjustAnalyticsProvider adjustAnalyticsProvider,
                                    @Nullable ComScoreAnalyticsProvider comScoreProvider,
                                    CrashlyticsAnalyticsProvider crashlyticsAnalyticsProvider) {
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
        this.analyticsProperties = analyticsProperties;
        this.eventLoggerAnalyticsProvider = eventLoggerProvider;
        this.playCountAnalyticsProvider = playCountProvider;
        this.localyticsAnalyticsProvider = localyticsProvider;
        this.adjustAnalyticsProvider = adjustAnalyticsProvider;
        this.comScoreAnalyticsProvider = comScoreProvider;
        this.promotedAnalyticsProvider = promotedProvider;
        this.crashlyticsAnalyticsProvider = crashlyticsAnalyticsProvider;
    }

    public List<AnalyticsProvider> getProviders() {
        initLogging();
        if (!analyticsProperties.isAnalyticsAvailable()) {
            return Collections.emptyList();
        }

        List<AnalyticsProvider> providers = getBaseProviders();
        if (sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)) {
            addOptInProviders(providers);
        }

        if (Fabric.isInitialized()) {
            providers.add(crashlyticsAnalyticsProvider);
        }

        return providers;
    }

    private void initLogging() {
        Log.d(analyticsProperties.toString());
        LocalyticsSession.setLoggingEnabled(analyticsProperties.isAnalyticsAvailable()
                && applicationProperties.useVerboseLogging());
    }

    // A list of providers that should always be enabled, regardless of user preference
    private List<AnalyticsProvider> getBaseProviders() {
        List<AnalyticsProvider> providers = new ArrayList<>(EXPECTED_PROVIDER_COUNT);
        providers.add(eventLoggerAnalyticsProvider);
        providers.add(playCountAnalyticsProvider);
        providers.add(promotedAnalyticsProvider);
        return providers;
    }

    private void addOptInProviders(List<AnalyticsProvider> providers) {
        providers.add(localyticsAnalyticsProvider);
        providers.add(adjustAnalyticsProvider);

        if (comScoreAnalyticsProvider != null) {
            providers.add(comScoreAnalyticsProvider);
        }
    }

}
