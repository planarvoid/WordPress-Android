package com.soundcloud.android.analytics;

import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.net.HttpHeaders;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        Log.d(EventTrackingManager.TAG, "Pushing " + events.size() + " new tracking events");

        List<TrackingRecord> successes = new ArrayList<>(events.size());

        for (TrackingRecord event : events) {
            try {
                Request request = buildRequest(event);

                final Response response = httpClient.newCall(request).execute();
                try {
                    final int status = response.code();
                    Log.d(EventTrackingManager.TAG, "Tracking event response: " + response.toString());

                    if (isSuccessCodeOrIgnored(status)) {
                        successes.add(event);
                    } else {
                        ErrorUtils.handleSilentException(EventTrackingManager.TAG,
                                                         new Exception(
                                                                 "Tracking request failed with unexpected status code: "
                                                                         + response.toString() + "; record = " + event));
                    }
                } finally {
                    response.body().close();
                }
            } catch (MalformedURLException e) {
                ErrorUtils.handleSilentException(EventTrackingManager.TAG, new Exception(event.toString(), e));
                successes.add(event); // no point in trying this event again
            } catch (IOException e) {
                Log.w(EventTrackingManager.TAG, "Failed with IOException pushing event: " + event, e);
            }
        }

        return successes;
    }

    private Request buildRequest(TrackingRecord event) throws MalformedURLException {
        final Request.Builder request = new Request.Builder();
        request.url(new URL(event.getData()));
        request.addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
        if (PromotedAnalyticsProvider.BACKEND_NAME.equals(event.getBackend())) {
            request.get();
        }
        return request.build();
    }

    private boolean isSuccessCodeOrIgnored(int status) {
        return status >= HttpStatus.OK && status < HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
