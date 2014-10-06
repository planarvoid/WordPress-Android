package com.soundcloud.android.analytics;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TrackingApiConnectionFactory {

    @Inject
    public TrackingApiConnectionFactory() {
        // for Dagger
    }

    public HttpURLConnection create(TrackingRecord event) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(event.getUrl()).openConnection();

        if (EventLoggerAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            connection.setRequestMethod("HEAD");
        } else if (PlayCountAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            connection.setRequestMethod("POST");
            connection.setFixedLengthStreamingMode(0);
            connection.setRequestProperty("Content-Length", "0");
        } else if (PromotedAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            connection.setRequestMethod("GET");
        }
        return connection;
    }
}
