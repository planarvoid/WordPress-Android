package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class TrackingApiFactoryTest {

    @Mock private OkHttpClient httpClient;
    @Mock private DeviceHelper deviceHelper;

    private TrackingApiFactory apiFactory;

    @Before
    public void setUp() throws Exception {
        apiFactory = new TrackingApiFactory(httpClient, deviceHelper);
    }

    @Test
    public void createsBatchApiForBoogaloo() {
        TrackingApi trackingApi = apiFactory.create(EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME);
        expect(trackingApi).toBeInstanceOf(BatchTrackingApi.class);
    }

    @Test
    public void createsSimpleApiForOtherBackends() {
        TrackingApi trackingApi = apiFactory.create(EventLoggerAnalyticsProvider.LEGACY_BACKEND_NAME);
        expect(trackingApi).toBeInstanceOf(SimpleTrackingApi.class);
    }

}