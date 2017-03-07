package com.soundcloud.android.analytics.performance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.firebase.FirebaseAnalyticsWrapper;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PerformanceAnalyticsProviderTest extends AndroidUnitTest {

    private static final MetricType METRIC_TYPE = MetricType.APP_ON_CREATE;
    private static final MetricParams METRIC_PARAMS = new MetricParams();
    private static final PerformanceEvent PERFORMANCE_EVENT = PerformanceEvent.create(METRIC_TYPE, METRIC_PARAMS);

    @Mock FirebaseAnalyticsWrapper firebaseAnalytics;
    @Mock FabricReporter fabricReporter;
    @Mock DeviceHelper deviceHelper;

    private PerformanceAnalyticsProvider provider;

    @Before
    public void setUp() {
        provider = new PerformanceAnalyticsProvider(firebaseAnalytics, fabricReporter, deviceHelper);

        when(deviceHelper.getAndroidReleaseVersion()).thenReturn("android");
        when(deviceHelper.getAppVersionName()).thenReturn("soundcloud");
    }

    @Test
    public void shouldLogEventsToFirebase() {
        provider.handlePerformanceEvent(PERFORMANCE_EVENT);

        verify(firebaseAnalytics).logEvent(METRIC_TYPE.toString(), METRIC_PARAMS.toBundle());
    }

    @Test
    public void shouldLogEventsToFabric() {
        provider.handlePerformanceEvent(PERFORMANCE_EVENT);

        verify(fabricReporter).post(any(Metric.class));
    }

}
