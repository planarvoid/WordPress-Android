package com.soundcloud.android.analytics;

import static com.soundcloud.java.collections.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.appboy.AppboyAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.crashlytics.FabricAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.settings.SettingKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsProviderFactoryTest {

    private AnalyticsProviderFactory factory;

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences analyticsSettings;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private AnalyticsProperties analyticsProperties;
    @Mock private EventLoggerAnalyticsProvider eventLoggerProvider;
    @Mock private Provider<AppboyAnalyticsProvider> appboyAnalyticsProvider;
    @Mock private ComScoreAnalyticsProvider comScoreProvider;
    @Mock private AdjustAnalyticsProvider adjustAnalyticsProvider;
    @Mock private FabricAnalyticsProvider fabricAnalyticsProvider;
    @Mock private PerformanceAnalyticsProvider performanceAnalyticsProvider;

    @Mock private EventTrackingManager eventTrackingManager;
    private PromotedAnalyticsProvider promotedProvider;

    private List<AnalyticsProvider> baseProviders;

    @Before
    public void setUp() throws Exception {
        promotedProvider = new PromotedAnalyticsProvider(eventTrackingManager);
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(appboyAnalyticsProvider.get()).thenReturn(mock(AppboyAnalyticsProvider.class));
        baseProviders = Arrays.asList(eventLoggerProvider, promotedProvider);
        factory = new AnalyticsProviderFactory(analyticsProperties, sharedPreferences,
                                               analyticsSettings, appboyAnalyticsProvider,
                                               adjustAnalyticsProvider, comScoreProvider,
                                               fabricAnalyticsProvider, performanceAnalyticsProvider,
                                               baseProviders);
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
        assertThat(providers).containsExactly(eventLoggerProvider, promotedProvider);
    }

    @Test
    public void getProvidersDoesNotReturnPromotedProviderWhenDisabled() {
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(false);
        when(analyticsSettings.getStringSet(eq(AnalyticsProviderFactory.DISABLED_PROVIDERS), anySet()))
                .thenReturn(newHashSet(PromotedAnalyticsProvider.class.getName()));

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(eventLoggerProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersWhenDoNotTrackIsNotSet() {
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(
                eventLoggerProvider,
                promotedProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider,
                appboyAnalyticsProvider.get(),
                performanceAnalyticsProvider,
                comScoreProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptComScoreWhenItFailedToInitialize() {
        factory = new AnalyticsProviderFactory(analyticsProperties, sharedPreferences,
                                               analyticsSettings, appboyAnalyticsProvider,
                                               adjustAnalyticsProvider, null, fabricAnalyticsProvider,
                performanceAnalyticsProvider, baseProviders);
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(
                eventLoggerProvider,
                promotedProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider,
                appboyAnalyticsProvider.get(),
                performanceAnalyticsProvider);
    }

}
