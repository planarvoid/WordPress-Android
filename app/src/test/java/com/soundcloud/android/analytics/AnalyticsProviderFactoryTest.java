package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.adjust.AdjustAnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.settings.GeneralSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
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
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        when(featureFlags.isEnabled(Feature.ADJUST_TRACKING)).thenReturn(true);
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences,
                eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, adjustAnalyticsProvider, comScoreProvider, featureFlags);
    }

    @Test
    public void getProvidersReturnsNoProvidersIfAnalyticsIsNotAvailable() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        expect(factory.getProviders()).toNumber(0);
    }

    @Test
    public void getProvidersReturnsBaseProvidersWhenDoNotTrackIsSet() {
        when(sharedPreferences.getBoolean(GeneralSettings.ANALYTICS_ENABLED, true)).thenReturn(false);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersWhenDoNotTrackIsNotSet() {
        when(sharedPreferences.getBoolean(GeneralSettings.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider, localyticsProvider, adjustAnalyticsProvider, comScoreProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptComScoreWhenItFailedToInitialize() {
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences, eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, adjustAnalyticsProvider, null, featureFlags);
        when(sharedPreferences.getBoolean(GeneralSettings.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider, localyticsProvider, adjustAnalyticsProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptAdjustWhenFeatureIsDisabled() {
        when(sharedPreferences.getBoolean(GeneralSettings.ANALYTICS_ENABLED, true)).thenReturn(true);
        when(featureFlags.isEnabled(Feature.ADJUST_TRACKING)).thenReturn(false);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider, localyticsProvider, comScoreProvider);
    }

    @Test
    public void getProvidersEnablesLocalyticsLoggingWhenVerboseLoggingIsOn() {
        when(applicationProperties.useVerboseLogging()).thenReturn(true);

        factory.getProviders();

        expect(LocalyticsSession.isLoggingEnabled()).toBeTrue();
    }

    @Test
    public void getProvidersDisablesLocalyticsLoggingWhenVerboseLoggingIsOff() {
        when(applicationProperties.useVerboseLogging()).thenReturn(false);

        factory.getProviders();

        expect(LocalyticsSession.isLoggingEnabled()).toBeFalse();
    }
}