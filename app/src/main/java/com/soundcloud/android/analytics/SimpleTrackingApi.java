package com.soundcloud.android.analytics;

import com.google.common.net.HttpHeaders;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Network facade for event tracking based on URLConnection. Processes a list of locally persisted events to be
 * uploaded to a tracking backend in an efficient manner.
 */
class SimpleTrackingApi implements TrackingApi {

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;

    @Inject
    SimpleTrackingApi(OkHttpClient httpClient, DeviceHelper deviceHelper) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
    }

    /**
     * @return a list of successfully submitted events
     */
    @Override
    public List<TrackingRecord> pushToRemote(List<TrackingRecord> events) {
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
                                new Exception("Tracking request failed with unexpected status code: "
                                        + response.toString() + "; record = " + event));
                    }
                } finally {
                    response.body().close();
                }
            } catch (MalformedURLException e) {
                ErrorUtils.handleSilentException(EventTracker.TAG, new Exception(event.toString(), e));
                successes.add(event); // no point in trying this event again
            } catch (IOException e) {
                Log.w(EventTracker.TAG, "Failed with IOException pushing event: " + event, e);
            }
        }

        return successes;
    }

    private Request buildRequest(TrackingRecord event) throws MalformedURLException {
        final Request.Builder request = new Request.Builder();
        request.url(new URL(event.getData()));
        request.addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());

        if (PlayCountAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
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
