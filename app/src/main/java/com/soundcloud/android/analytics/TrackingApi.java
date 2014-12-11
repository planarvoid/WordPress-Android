package com.soundcloud.android.analytics;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.playcounts.PlayCountAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Network facade for event tracking based on URLConnection. Processes a list of locally persisted events to be
 * uploaded to a tracking backend in an efficient manner.
 */
class TrackingApi {
    private static final int READ_TIMEOUT = 5;
    private static final int CONNECT_TIMEOUT = 10;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;

    @Inject
    TrackingApi(OkHttpClient httpClient, DeviceHelper deviceHelper) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
        httpClient.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * @return a list of successfully submitted events
     */
    List<TrackingRecord> pushToRemote(List<TrackingRecord> events) {
        Log.d(EventTracker.TAG, "Pushing " + events.size() + " new tracking events");

        List<TrackingRecord> successes = new ArrayList<>(events.size());

        for (TrackingRecord event : events) {
            try {
                Request request = buildRequest(event);

                final Response response = httpClient.newCall(request).execute();
                try {
                    final int status = response.code();
                    Log.d(EventTracker.TAG, "Tracking event response: " + response.toString());

                    if (isSuccessCodeOrIgnored(status)) {
                        successes.add(event);
                    } else {
                        ErrorUtils.handleSilentException(EventTracker.TAG,
                                new Exception("Tracking request failed with unexpected status code: " + response.toString()));
                    }
                } finally {
                    response.body().close();
                }
            } catch (IOException e) {
                Log.w(EventTracker.TAG, "Failed with IOException pushing event: " + event, e);
                ErrorUtils.handleSilentException(EventTracker.TAG,
                        new Exception("Tracking request failed with IOException: " + e.getClass().getName(), e));
            }
        }

        return successes;
    }

    private Request buildRequest(TrackingRecord event) throws IOException {
        final Request.Builder request = new Request.Builder();
        request.url(event.getUrl());
        request.addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());

        if (EventLoggerAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            request.head();
        } else if (PlayCountAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            request.post(null);
            request.addHeader("Content-Length", "0");
        } else if (PromotedAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            request.get();
        }
        return request.build();
    }

    private boolean isSuccessCodeOrIgnored(int status) {
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
