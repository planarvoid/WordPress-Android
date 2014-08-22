package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.localytics.android.Constants;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    @Before
    public void setUp() throws Exception {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(true);
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences, eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, comScoreProvider);
    }

    @Test
    public void getProvidersReturnsNoProvidersIfAnalyticsIsNotAvailable() {
        when(analyticsProperties.isAnalyticsAvailable()).thenReturn(false);

        expect(factory.getProviders()).toNumber(0);
    }

    @Test
    public void getProvidersReturnsBaseProvidersWhenDoNotTrackIsSet() {
        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(false);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersWhenDoNotTrackIsNotSet() {
        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider, localyticsProvider, comScoreProvider);
    }

    @Test
    public void getProvidersReturnsAllProvidersExceptComScoreWhenItFailedToInitialize() {
        factory = new AnalyticsProviderFactory(analyticsProperties, applicationProperties, sharedPreferences, eventLoggerProvider, playCountProvider, localyticsProvider, promotedProvider, null);
        when(sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true)).thenReturn(true);

        List<AnalyticsProvider> providers = factory.getProviders();
        expect(providers).toContainExactly(eventLoggerProvider, playCountProvider, promotedProvider, localyticsProvider);
    }

    @Test
    public void getProvidersEnablesLocalyticsLoggingWhenVerboseLoggingIsOn() {
        when(applicationProperties.useVerboseLogging()).thenReturn(true);

        factory.getProviders();

        expect(Constants.IS_LOGGABLE).toBeTrue();
    }

    @Test
    public void getProvidersDisablesLocalyticsLoggingWhenVerboseLoggingIsOff() {
        when(applicationProperties.useVerboseLogging()).thenReturn(false);

        factory.getProviders();

        expect(Constants.IS_LOGGABLE).toBeFalse();
    }
}