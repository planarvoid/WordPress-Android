package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingEvents.*;

import com.integralblue.httpresponsecache.compat.Charsets;
import org.apache.http.HttpStatus;

import android.database.Cursor;
import android.util.Log;
import android.util.Pair;

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
    private static final int READ_TIMEOUT = 5 * 1000;
    private static final int CONNECT_TIMEOUT = 10 * 1000;

    private final String mAppId;

    public PlayEventTrackingApi(String appId) {
        mAppId = appId;
    }

    /**
     * Submit play events to the server using the given urls
     * @param urlPairs pair [id, url] of the event to push
     * @return  a list of successfully submitted ids
     */
    public String[] pushToRemote(List<Pair<String, String>> urlPairs) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Pushing " + urlPairs.size() + " new tracking events");

        List<String> successes = new ArrayList<String>(urlPairs.size());
        HttpURLConnection connection = null;

        for (Pair<String,String> urlPair : urlPairs){
            try {
                final String url = urlPair.second;
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "logging "+url);
                connection = (HttpURLConnection) new URL(url).openConnection();

                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.connect();

                final int response = connection.getResponseCode();

                if (response == HttpStatus.SC_OK) {
                    successes.add(urlPair.first);
                } else {
                    Log.w(TAG, "unexpected status code: " + response);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed pushing play event " + urlPair);
            }
        }

        if (connection != null) {
            connection.disconnect();
        }

        return successes.toArray(new String[successes.size()]);
    }

    String buildUrl(Cursor trackingData) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(ENDPOINT);
        sb.append("?client_id=").append(mAppId);
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
