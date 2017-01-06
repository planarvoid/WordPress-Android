package com.soundcloud.android.analytics.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

public class FirebaseAnalyticsProviderTest extends AndroidUnitTest {

    private static final boolean IS_USER_LOGGED_IN = true;
    private static final long TIME_MILLIS = 10L;
    private static final String APP_VERSION = "debug";
    private static final String DEVICE_NAME = "Xiaomi";
    private static final String ANDROID_VERSION = "7.0.0";

    private FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Mock private FirebaseAnalyticsWrapper firebaseAnalytics;

    @Before
    public void setUp() {
        firebaseAnalyticsProvider = new FirebaseAnalyticsProvider(firebaseAnalytics);
    }

    @Test
    public void shouldLogPerformanceEventUsingFirebase() {
        final PerformanceEvent performanceEvent = mock(PerformanceEvent.class);
        when(performanceEvent.name()).thenReturn(PerformanceEvent.class.getSimpleName());

        firebaseAnalyticsProvider.handlePerformanceEvent(performanceEvent);

        verify(firebaseAnalytics).logEvent(anyString(), any(Bundle.class));
    }

    @Test
    public void shouldBuildEventDataWithCorrectParameters() {
        final Bundle eventData = firebaseAnalyticsProvider.buildFirebaseEventData(createPerformanceEvent());

        assertThat(eventData.getBoolean(FirebaseAnalyticsProvider.DATA_IS_USER_LOGGED_IN)).isEqualTo(IS_USER_LOGGED_IN);
        assertThat(eventData.getLong(FirebaseAnalyticsProvider.DATA_TIME_MILLIS)).isEqualTo(TIME_MILLIS);
        assertThat(eventData.getString(FirebaseAnalyticsProvider.DATA_APP_VERSION)).isEqualTo(APP_VERSION);
        assertThat(eventData.getString(FirebaseAnalyticsProvider.DATA_DEVICE_NAME)).isEqualTo(DEVICE_NAME);
        assertThat(eventData.getString(FirebaseAnalyticsProvider.DATA_ANDROID_VERSION)).isEqualTo(ANDROID_VERSION);
    }

    private PerformanceEvent createPerformanceEvent() {
        return PerformanceEvent.forApplicationStartupTime(IS_USER_LOGGED_IN,
                TIME_MILLIS, APP_VERSION, DEVICE_NAME, ANDROID_VERSION);
    }
}
