package com.soundcloud.android.tracking.eventlogger;

import com.integralblue.httpresponsecache.compat.Charsets;
import com.soundcloud.android.provider.DBHelper;

import android.database.Cursor;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public class PlayEventTrackingApi {

    private static final String TAG = PlayEventTrackingApi.class.getSimpleName();
    private static final String ENDPOINT = "https://eventlogger.soundcloud.com/audio";
    private static final int CONNECTION_TIMEOUT = 10 * 1000;

    private final String mClientId;

    public PlayEventTrackingApi(String clientId) {
        mClientId = clientId;
    }

    public void pushToRemote(Cursor trackingData) {
        Log.d(TAG, "Pushing " + trackingData.getCount() + " new tracking events");

        String url = null;
        while (trackingData.moveToNext()) {
            HttpsURLConnection connection = null;
            try {
                url = buildUrl(trackingData);
                connection = (HttpsURLConnection) new URL(url).openConnection();

                connection.setReadTimeout(CONNECTION_TIMEOUT);

                connection.connect();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Failed pushing play event " + url);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    public String buildUrl(Cursor trackingData) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(ENDPOINT);
        sb.append("?client_id=" + mClientId);
        long timestamp = trackingData.getLong(trackingData.getColumnIndex(DBHelper.TrackingEvents.TIMESTAMP));
        sb.append("&ts=" + timestamp);
        String action = trackingData.getString(trackingData.getColumnIndex(DBHelper.TrackingEvents.ACTION));
        sb.append("&action=" + action);
        String userUrn = trackingData.getString(trackingData.getColumnIndex(DBHelper.TrackingEvents.USER_URN));
        sb.append("&user=" + URLEncoder.encode(userUrn, Charsets.UTF_8.name()));
        String soundUrn = trackingData.getString(trackingData.getColumnIndex(DBHelper.TrackingEvents.SOUND_URN));
        sb.append("&sound=" + URLEncoder.encode(soundUrn, Charsets.UTF_8.name()));
        long soundDuration = trackingData.getLong(trackingData.getColumnIndex(DBHelper.TrackingEvents.SOUND_DURATION));
        sb.append("&duration=" + soundDuration);

        //TODO: url, level

        return sb.toString();
    }

}
