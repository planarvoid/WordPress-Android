package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.crashlytics.FabricAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.settings.SettingKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsProviderFactoryTest {

    private AnalyticsProviderFactory factory;

    @Mock private SharedPreferences sharedPreferences;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private AnalyticsProperties analyticsProperties;
    @Mock private EventLoggerAnalyticsProvider eventLoggerProvider;
    @Mock private PlayCountAnalyticsProvider playCountProvider;
    @Mock private LocalyticsAnalyticsProvider localyticsProvider;
    @Mock private PromotedAnalyticsProvider promotedProvider;
    @Mock private ComScoreAnalyticsProvider comScoreProvider;
    @Mock private AdjustAnalyticsProvider adjustAnalyticsProvider;
    @Mock private FabricAnalyticsProvider fabricAnalyticsProvider;

    @Before
    public void setUp() throws Exception {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences,
                eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, adjustAnalyticsProvider, comScoreProvider, fabricAnalyticsProvider);
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
                localyticsProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider,
                comScoreProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptComScoreWhenItFailedToInitialize() {
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences,
                eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, adjustAnalyticsProvider,
                null, fabricAnalyticsProvider);
        when(sharedPreferences.getBoolean(SettingKey.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        assertThat(providers).containsExactly(
                eventLoggerProvider,
                playCountProvider,
                promotedProvider,
                localyticsProvider,
                adjustAnalyticsProvider,
                fabricAnalyticsProvider);
    }

    @Test
    public void getProvidersEnablesLocalyticsLoggingWhenVerboseLoggingIsOn() {
        when(applicationProperties.useVerboseLogging()).thenReturn(true);

        factory.getProviders();

        assertThat(LocalyticsSession.isLoggingEnabled()).isTrue();
    }

    @Test
    public void getProvidersDisablesLocalyticsLoggingWhenVerboseLoggingIsOff() {
        when(applicationProperties.useVerboseLogging()).thenReturn(false);

        factory.getProviders();

        assertThat(LocalyticsSession.isLoggingEnabled()).isFalse();
    }
}