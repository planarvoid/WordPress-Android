package com.soundcloud.android.analytics.localytics;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.events.DeviceMetricsEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderDeviceMetricsTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock private LocalyticsAmpSession localyticsSession;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null, 123L);
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeLessThanOneMb() {
        verifyDatabaseSizeEvent(1, "<1mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenOneAndFiveMb() {
        verifyDatabaseSizeEvent(1024 * 1024 + 1, "1mb to 5mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenFiveAndTenMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 5 + 1,"5mb to 10mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTenAndTwentyMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 10 + 1,"10mb to 20mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTwentyAndFiftyMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 20 + 1,"20mb to 50mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenFiftyAndOneHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 50 + 1,"50mb to 100mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenOneHundredAndTwoHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 100 + 1,"100mb to 200mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTwoHundredAndFiveHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 200 + 1,"200mb to 500mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeGreaterThanOneHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 500 + 1,">500mb");
    }

    @SuppressWarnings("unchecked")
    private void verifyDatabaseSizeEvent(long dbSize, String eventSizeReport){
        DeviceMetricsEvent event = new DeviceMetricsEvent(dbSize);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq("Device Metrics"),
                (Map<String, String>) argThat(hasEntry("database_size", eventSizeReport)));
    }

}