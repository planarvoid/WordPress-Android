package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

public class TrackingApiConnectionFactoryTest {

    private TrackingApiConnectionFactory factory = new TrackingApiConnectionFactory();

    @Test
    public void shouldOpenConnectionToUrlSpecifiedInEvent() throws IOException {
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getURL().toString()).toEqual("http://url");
    }

    @Test
    public void shouldSetupConnectionForEventLogger() throws IOException {
        TrackingRecord event = new TrackingRecord(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getRequestMethod()).toEqual("HEAD");
    }

    @Test
    public void promotedExpectsGETRequests() throws IOException {
        TrackingRecord event = new TrackingRecord(1L, PromotedAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getRequestMethod()).toEqual("GET");
    }

    @Test
    public void shouldSetupConnectionForPlayCountsTrackingViaPublicApi() throws IOException {
        TrackingRecord event = new TrackingRecord(1L, PlayCountAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getRequestMethod()).toEqual("POST");
    }

}