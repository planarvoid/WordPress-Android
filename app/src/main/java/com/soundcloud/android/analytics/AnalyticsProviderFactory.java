package com.soundcloud.android.analytics;

import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.crashlytics.FabricAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalyticsProviderFactory {

    private static final int EXPECTED_PROVIDER_COUNT = 8;

    private final SharedPreferences sharedPreferences;
    private final AnalyticsProperties analyticsProperties;
    private final FeatureFlags featureFlags;
    private final EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider;
    private final PlayCountAnalyticsProvider playCountAnalyticsProvider;
    private final PromotedAnalyticsProvider promotedAnalyticsProvider;
    private final Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider;
    private final AdjustAnalyticsProvider adjustAnalyticsProvider;
    private final FabricAnalyticsProvider fabricAnalyticsProvider;

    @Nullable private final ComScoreAnalyticsProvider comScoreAnalyticsProvider;

    @Inject
    public AnalyticsProviderFactory(AnalyticsProperties analyticsProperties,
                                    SharedPreferences sharedPreferences,
                                    FeatureFlags featureFlags,
                                    EventLoggerAnalyticsProvider eventLoggerProvider,
                                    PlayCountAnalyticsProvider playCountProvider,
                                    Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider,
                                    PromotedAnalyticsProvider promotedProvider,
                                    AdjustAnalyticsProvider adjustAnalyticsProvider,
                                    @Nullable ComScoreAnalyticsProvider comScoreProvider,
                                    FabricAnalyticsProvider fabricAnalyticsProvider) {
        this.sharedPreferences = sharedPreferences;
        this.analyticsProperties = analyticsProperties;
        this.featureFlags = featureFlags;
        this.eventLoggerAnalyticsProvider = eventLoggerProvider;
        this.playCountAnalyticsProvider = playCountProvider;
        this.appboyAnalyticsProvider = appboyAnalyticsProvider;
        this.adjustAnalyticsProvider = adjustAnalyticsProvider;
        this.comScoreAnalyticsProvider = comScoreProvider;
        this.promotedAnalyticsProvider = promotedProvider;
        this.fabricAnalyticsProvider = fabricAnalyticsProvider;
    }

    public List<AnalyticsProvider> getProviders() {
        if (!analyticsProperties.isAnalyticsAvailable()) {
            return Collections.emptyList();
        }

        List<AnalyticsProvider> providers = getBaseProviders();
        if (sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)) {
            addOptInProviders(providers);
        }

        return providers;
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
        providers.add(adjustAnalyticsProvider);
        providers.add(fabricAnalyticsProvider);

        if (featureFlags.isEnabled(Flag.APPBOY)) {
            providers.add(appboyAnalyticsProvider.get());
        }

        if (comScoreAnalyticsProvider != null) {
            providers.add(comScoreAnalyticsProvider);
        }
    }
}
