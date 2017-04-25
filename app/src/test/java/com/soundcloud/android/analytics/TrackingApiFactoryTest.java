package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TrackingApiFactoryTest {
    private static final String URL = "testurl";

    @Mock private OkHttpClient httpClient;
    @Mock private DeviceHelper deviceHelper;

    private TrackingApiFactory apiFactory;

    @Before
    public void setUp() throws Exception {
        apiFactory = new TrackingApiFactory(httpClient, deviceHelper, URL);
    }

    @Test
    public void createsBatchApiForEventGateway() {
        TrackingApi trackingApi = apiFactory.create(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        assertThat(trackingApi).isInstanceOf(BatchTrackingApi.class);
    }

    @Test
    public void createsSimpleApiForOtherBackends() {
        TrackingApi trackingApi = apiFactory.create(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(trackingApi).isInstanceOf(SimpleTrackingApi.class);
    }

}
