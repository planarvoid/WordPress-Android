package com.soundcloud.android.analytics.performance;

import static com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider.DATA_ANDROID_VERSION;
import static com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider.DATA_APP_VERSION;
import static com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider.DATA_DEVICE_NAME;
import static com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider.DATA_IS_USER_LOGGED_IN;
import static com.soundcloud.android.analytics.performance.PerformanceAnalyticsProvider.DATA_TIME_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.firebase.FirebaseAnalyticsWrapper;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

import java.util.Set;

public class PerformanceAnalyticsProviderTest extends AndroidUnitTest {

    private static final boolean IS_USER_LOGGED_IN = true;
    private static final long TIME_MILLIS = 10L;
    private static final String APP_VERSION = "debug";
    private static final String DEVICE_NAME = "Xiaomi";
    private static final String ANDROID_VERSION = "7.0.0";

    private PerformanceAnalyticsProvider performanceAnalyticsProvider;

    @Mock private FirebaseAnalyticsWrapper firebaseAnalytics;
    @Mock private FabricReporter fabricReporter;

    @Before
    public void setUp() {
        performanceAnalyticsProvider = new PerformanceAnalyticsProvider(firebaseAnalytics, fabricReporter);
    }

    @Test
    public void shouldLogPerformanceEventUsingFirebaseAndFabric() {
        final PerformanceEvent performanceEvent = mock(PerformanceEvent.class);
        when(performanceEvent.name()).thenReturn(PerformanceEvent.class.getSimpleName());

        performanceAnalyticsProvider.handlePerformanceEvent(performanceEvent);

        verify(firebaseAnalytics).logEvent(anyString(), any(Bundle.class));
        verify(fabricReporter).post(any(Metric.class));
    }

    @Test
    public void shouldBuildFirebaseEventDataWithCorrectParameters() {
        final Bundle eventData = performanceAnalyticsProvider.buildFirebaseEventData(createAppStartupPerformanceEvent());

        assertThat(eventData.getBoolean(DATA_IS_USER_LOGGED_IN)).isEqualTo(IS_USER_LOGGED_IN);
        assertThat(eventData.getLong(DATA_TIME_MILLIS)).isEqualTo(TIME_MILLIS);
        assertThat(eventData.getString(DATA_APP_VERSION)).isEqualTo(APP_VERSION);
        assertThat(eventData.getString(DATA_DEVICE_NAME)).isEqualTo(DEVICE_NAME);
        assertThat(eventData.getString(DATA_ANDROID_VERSION)).isEqualTo(ANDROID_VERSION);
    }

    @Test
    public void shouldBuildFabricMetricDataWithCorrectParameters() {
        final PerformanceEvent performanceEvent = createAppStartupPerformanceEvent();
        final Metric appStartupMetric = performanceAnalyticsProvider.buildFabricAppStartupMetric(performanceEvent);
        final Set<DataPoint<?>> dataPoints = appStartupMetric.dataPoints();

        final DataPoint<Number> timeMillis = DataPoint.numeric(DATA_TIME_MILLIS, performanceEvent.timeMillis());
        final DataPoint<String> isUserLoggedIn = DataPoint.string(DATA_IS_USER_LOGGED_IN, String.valueOf(performanceEvent.userLoggedIn()));
        final DataPoint<String> appVersionName = DataPoint.string(DATA_APP_VERSION, performanceEvent.appVersionName());
        final DataPoint<String> androidVersion = DataPoint.string(DATA_ANDROID_VERSION, performanceEvent.androidVersion());

        assertThat(dataPoints.contains(timeMillis)).isTrue();
        assertThat(dataPoints.contains(isUserLoggedIn)).isTrue();
        assertThat(dataPoints.contains(appVersionName)).isTrue();
        assertThat(dataPoints.contains(androidVersion)).isTrue();
    }

    private PerformanceEvent createAppStartupPerformanceEvent() {
        return PerformanceEvent.forApplicationStartupTime(IS_USER_LOGGED_IN,
                TIME_MILLIS, APP_VERSION, DEVICE_NAME, ANDROID_VERSION);
    }
}
