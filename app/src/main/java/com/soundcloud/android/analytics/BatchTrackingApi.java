package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.net.HttpHeaders;
import com.soundcloud.java.strings.Charsets;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class BatchTrackingApi implements TrackingApi {

    static final String CONTENT_TYPE = "application/json";

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;
    private final String batchUrl;
    private final int batchSize;

    @Inject
    BatchTrackingApi(OkHttpClient httpClient, DeviceHelper deviceHelper, String batchUrl, int batchSize) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
        this.batchUrl = batchUrl;
        this.batchSize = batchSize;
    }

    @Override
    public List<TrackingRecord> pushToRemote(List<TrackingRecord> events) {
        List<TrackingRecord> successes = new ArrayList<>(events.size());

        try {
            int batchIndex = 0;
            do {
                final int startIndex = batchIndex * batchSize;
                final int endIndex = Math.min(events.size(), startIndex + batchSize);
                sendBatch(events.subList(startIndex, endIndex), successes);
                batchIndex++;
            } while (batchIndex * batchSize < events.size());

        } catch (IOException e) {
            Log.w(EventTrackingManager.TAG, "Failed with IOException pushing event count: " + events.size(), e);
        } catch (JSONException e) {
            ErrorUtils.handleSilentException(e);
            Log.e(EventTrackingManager.TAG, "Failed with JSONException, pushing event count: " + events.size(), e);
        }
        return successes;
    }

    private void sendBatch(List<TrackingRecord> events,
                           List<TrackingRecord> successes) throws IOException, JSONException {
        Request request = buildRequest(events);

        final Response response = httpClient.newCall(request).execute();
        try {
            final int status = response.code();
            Log.d(EventTrackingManager.TAG, "Tracking event response: " + response.toString()
                    + ";body=" + response.body().string());
            if (isSuccessCodeOrIgnored(status)) {
                successes.addAll(events);
            } else {
                ErrorUtils.handleSilentException(EventTrackingManager.TAG,
                                                 new Exception("Tracking request failed with unexpected status code: "
                                                                       + response.toString() + "; recordCount = " + events
                                                         .size()));
            }
        } finally {
            response.body().close();
        }
    }

    private Request buildRequest(List<TrackingRecord> events) throws IOException, JSONException {
        final Request.Builder request = new Request.Builder();
        request.url(batchUrl);
        request.addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
        request.post(createBody(events));
        return request.build();
    }

    private RequestBody createBody(List<TrackingRecord> events) throws IOException, JSONException {
        final String body = new JSONArray(getEventJsonObjects(events)).toString();
        Log.d(EventTrackingManager.TAG, "event payload: " + body);
        return RequestBody.create(MediaType.parse(CONTENT_TYPE), body.getBytes(Charsets.UTF_8.name()));
    }

    private List<JSONObject> getEventJsonObjects(List<TrackingRecord> events) throws JSONException {
        final List<JSONObject> eventObjects = new ArrayList<>(events.size());
        for (TrackingRecord event : events) {
            eventObjects.add(new JSONObject(event.getData()));
        }
        return eventObjects;
    }

    private boolean isSuccessCodeOrIgnored(int status) {
        return status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_INTERNAL_ERROR;
    }

}
