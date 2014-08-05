package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

public class TrackingApiConnectionFactoryTest {

    private TrackingApiConnectionFactory factory = new TrackingApiConnectionFactory();

    @Test
    public void shouldOpenConnectionToUrlSpecifiedInEvent() throws IOException {
        TrackingEvent event = new TrackingEvent(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getURL().toString()).toEqual("http://url");
    }

    @Test
    public void shouldSetupConnectionForEventLogger() throws IOException {
        TrackingEvent event = new TrackingEvent(1L, EventLoggerAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getRequestMethod()).toEqual("HEAD");
    }

    @Test
    public void shouldSetupConnectionForPlayCountsTrackingViaPublicApi() throws IOException {
        TrackingEvent event = new TrackingEvent(1L, PlayCountAnalyticsProvider.BACKEND_NAME, "http://url");

        HttpURLConnection connection = factory.create(event);

        expect(connection.getRequestMethod()).toEqual("POST");
    }

}