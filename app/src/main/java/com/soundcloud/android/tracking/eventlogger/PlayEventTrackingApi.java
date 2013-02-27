package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingEvents.*;

import com.integralblue.httpresponsecache.compat.Charsets;
import com.soundcloud.android.utils.IOUtils;
import org.apache.http.HttpStatus;

import android.database.Cursor;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PlayEventTrackingApi {

    private static final String TAG = PlayEventTrackingApi.class.getSimpleName();
    private static final String ENDPOINT = "http://eventlogger.soundcloud.com/audio";
    private static final int CONNECTION_TIMEOUT = 10 * 1000;

    private final String mClientId;

    public PlayEventTrackingApi(String clientId) {
        mClientId = clientId;
    }

    /**
     * @param trackingData a cursor with the tracking data to be submitted
     * @return a list of successfully submitted ids
     */
    public String[] pushToRemote(Cursor trackingData) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Pushing " + trackingData.getCount() + " new tracking events");

        List<String> successes = new ArrayList<String>(trackingData.getCount());
        String url = null;
        HttpURLConnection connection = null;

        while (trackingData.moveToNext()) {
            try {
                url = buildUrl(trackingData);
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "logging "+url);
                connection = (HttpURLConnection) new URL(url).openConnection();

                connection.setReadTimeout(CONNECTION_TIMEOUT);
                connection.connect();

                final int response = connection.getResponseCode();

                if (response == HttpStatus.SC_OK) {
                    successes.add(trackingData.getString(trackingData.getColumnIndex(PlayEventTracker.TrackingEvents._ID)));
                } else {
                    Log.w(TAG, "unexpected status code: " + response);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed pushing play event " + url);
            } finally {
                // consume the response or HTTP pipelining will not work
                IOUtils.consumeStream(connection);
            }
        }

        if (connection != null) {
            connection.disconnect();
        }

        return successes.toArray(new String[successes.size()]);
    }

    public String buildUrl(Cursor trackingData) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(ENDPOINT);
        sb.append("?client_id=").append(mClientId);
        long timestamp = trackingData.getLong(trackingData.getColumnIndex(TIMESTAMP));
        sb.append("&ts=").append(timestamp);
        String action = trackingData.getString(trackingData.getColumnIndex(ACTION));
        sb.append("&action=").append(action);
        String userUrn = trackingData.getString(trackingData.getColumnIndex(USER_URN));
        sb.append("&user=").append(URLEncoder.encode(userUrn, Charsets.UTF_8.name()));
        String soundUrn = trackingData.getString(trackingData.getColumnIndex(SOUND_URN));
        sb.append("&sound=").append(URLEncoder.encode(soundUrn, Charsets.UTF_8.name()));
        long soundDuration = trackingData.getLong(trackingData.getColumnIndex(SOUND_DURATION));
        sb.append("&duration=").append(soundDuration);

        //TODO: url, level

        return sb.toString();
    }
}
