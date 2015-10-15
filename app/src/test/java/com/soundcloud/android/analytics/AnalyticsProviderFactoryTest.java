package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

import javax.inject.Provider;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsProviderFactoryTest {

    private AnalyticsProviderFactory factory;

    @Mock private SharedPreferences sharedPreferences;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private AnalyticsProperties analyticsProperties;
    @Mock private FeatureFlags featureFlags;
    @Mock private EventLoggerAnalyticsProvider eventLoggerProvider;
    @Mock private PlayCountAnalyticsProvider playCountProvider;
    @Mock private Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider;
    @Mock private PromotedAnalyticsProvider promotedProvider;
    @Mock private ComScoreAnalyticsProvider comScoreProvider;
    @Mock private AdjustAnalyticsProvider adjustAnalyticsProvider;
    @Mock private FabricAnalyticsProvider fabricAnalyticsProvider;

    @Before
    public void setUp() throws Exception {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(featureFlags.isEnabled(Flag.APPBOY)).thenReturn(true);
        when(appboyAnalyticsProvider.get()).thenReturn(mock(AppboyAnalyticsProvider.class));
        factory = new AnalyticsProviderFactory(analyticsProperties, sharedPreferences,
                featureFlags, eventLoggerProvider, playCountProvider, appboyAnalyticsProvider, promotedProvider,
                adjustAnalyticsProvider, comScoreProvider, fabricAnalyticsProvider);
    }

    @Test
    public void getProvidersReturnsNoProvidersIfAnalyticsIsNotAvailable() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        assertThat(factory.getProviders()).isEmpty();
    }

    @Test
    public void getProvidersReturnsBaseProvidersWhenDoNotTrackIsSet() {
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(false);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(eventLoggerProvider, playCountProvider, promotedProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersWhenDoNotTrackIsNotSet() {
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(
                eventLoggerProvider,
                playCountProvider,
                promotedProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider,
                appboyAnalyticsProvider.get(),
                comScoreProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptComScoreWhenItFailedToInitialize() {
        factory = new AnalyticsProviderFactory(analyticsProperties, sharedPreferences,
                featureFlags, eventLoggerProvider, playCountProvider, appboyAnalyticsProvider,
                promotedProvider, adjustAnalyticsProvider, null, fabricAnalyticsProvider);
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(
                eventLoggerProvider,
                playCountProvider,
                promotedProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider,
                appboyAnalyticsProvider.get());
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptAppboyWhenFlagIsDisabled() {
        factory = new AnalyticsProviderFactory(analyticsProperties, sharedPreferences,
                featureFlags, eventLoggerProvider, playCountProvider, appboyAnalyticsProvider,
                promotedProvider, adjustAnalyticsProvider, null, fabricAnalyticsProvider);

        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);
        when(featureFlags.isEnabled(Flag.APPBOY)).thenReturn(false);

        List<AnalyticsProvider> providers = factory.getProviders();

        assertThat(providers).containsExactly(
                eventLoggerProvider,
                playCountProvider,
                promotedProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider);
    }

}
