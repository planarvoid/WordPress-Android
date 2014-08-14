package com.soundcloud.android.analytics;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Network facade for event tracking based on URLConnection. Processes a list of locally persisted events to be
 * uploaded to a tracking backend in an efficient manner.
 */
class TrackingApi {
    private static final int READ_TIMEOUT = 5 * 1000;
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final TrackingApiConnectionFactory connectionFactory;
    private final DeviceHelper deviceHelper;

    @Inject
    TrackingApi(TrackingApiConnectionFactory connectionFactory, DeviceHelper deviceHelper) {
        this.connectionFactory = connectionFactory;
        this.deviceHelper = deviceHelper;
    }

    /**
     * @return a list of successfully submitted events
     */
    List<TrackingEvent> pushToRemote(List<TrackingEvent> events) {
        Log.d(EventTracker.TAG, "Pushing " + events.size() + " new tracking events");

        List<TrackingEvent> successes = new ArrayList<TrackingEvent>(events.size());
        HttpURLConnection connection = null;

        for (TrackingEvent event : events) {
            try {
                connection = connectionFactory.create(event);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestProperty(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
                connection.connect();

                final int status = connection.getResponseCode();
                Log.d(EventTracker.TAG, connection.getRequestMethod() + " " + event.getUrl() + ": " + status);

                if (isSuccessCodeOrIgnored(status)) {
                    successes.add(event);
                } else {
                    ErrorUtils.handleSilentException(EventTracker.TAG,
                            new Exception("Tracking request failed with unexpected status code: " + status
                                    + "\nURL: " + connection.getURL()));
                }
            } catch (IOException e) {
                Log.w(EventTracker.TAG, "Failed pushing event " + event);
            }
        }

        if (connection != null) {
            connection.disconnect();
        }

        return successes;
    }

    private boolean isSuccessCodeOrIgnored(int status) {
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
