package com.soundcloud.android.analytics;

import static com.soundcloud.android.storage.StorageModule.ANALYTICS_SETTINGS;

import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.crashlytics.FabricAnalyticsProvider;
import com.soundcloud.android.analytics.firebase.FirebaseAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.settings.SettingKey;
import dagger.Lazy;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AnalyticsProviderFactory {

    private static final int EXPECTED_PROVIDER_COUNT = 7;
    public static final String DISABLED_PROVIDERS = "disabled_providers";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences analyticsSettings;
    private final AnalyticsProperties analyticsProperties;
    private final Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider;
    private final AdjustAnalyticsProvider adjustAnalyticsProvider;
    private final FabricAnalyticsProvider fabricAnalyticsProvider;
    private final Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;
    private final List<AnalyticsProvider> baseProviders;
    private final ApplicationProperties applicationProperties;

    @Nullable private final ComScoreAnalyticsProvider comScoreAnalyticsProvider;

    @Inject
    public AnalyticsProviderFactory(AnalyticsProperties analyticsProperties,
                                    SharedPreferences sharedPreferences,
                                    @Named(ANALYTICS_SETTINGS) SharedPreferences analyticsSettings,
                                    Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider,
                                    AdjustAnalyticsProvider adjustAnalyticsProvider,
                                    @Nullable ComScoreAnalyticsProvider comScoreProvider,
                                    FabricAnalyticsProvider fabricAnalyticsProvider,
                                    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider,
                                    @Named(AnalyticsModule.BASE_PROVIDERS) List<AnalyticsProvider> baseProviders,
                                    ApplicationProperties applicationProperties) {
        this.sharedPreferences = sharedPreferences;
        this.analyticsProperties = analyticsProperties;
        this.analyticsSettings = analyticsSettings;
        this.appboyAnalyticsProvider = appboyAnalyticsProvider;
        this.adjustAnalyticsProvider = adjustAnalyticsProvider;
        this.comScoreAnalyticsProvider = comScoreProvider;
        this.fabricAnalyticsProvider = fabricAnalyticsProvider;
        this.firebaseAnalyticsProvider = firebaseAnalyticsProvider;
        this.baseProviders = baseProviders;
        this.applicationProperties = applicationProperties;
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
        final Set<String> disabledProviders = analyticsSettings.getStringSet(DISABLED_PROVIDERS,
                                                                             Collections.<String>emptySet());
        for (AnalyticsProvider provider : baseProviders) {
            if (!disabledProviders.contains(provider.getClass().getName())) {
                providers.add(provider);
            }
        }
        return providers;
    }

    private void addOptInProviders(List<AnalyticsProvider> providers) {
        providers.add(adjustAnalyticsProvider);
        providers.add(fabricAnalyticsProvider);
        providers.add(appboyAnalyticsProvider.get());

        if (applicationProperties.isAlphaBuild() || applicationProperties.isDebugBuild()) {
            providers.add(firebaseAnalyticsProvider.get());
        }

        if (comScoreAnalyticsProvider != null) {
            providers.add(comScoreAnalyticsProvider);
        }
    }
}
