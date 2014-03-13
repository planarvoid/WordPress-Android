package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerDbHelper.TrackingEvents.PARAMS;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerDbHelper.TrackingEvents.PATH;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.api.http.HttpURLConnectionFactory;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class EventLoggerApi {

    private static final String TAG = EventLoggerApi.class.getSimpleName();
    private static final String ENDPOINT = "http://eventlogger.soundcloud.com";

    static final int READ_TIMEOUT = 5 * 1000;
    static final int CONNECT_TIMEOUT = 10 * 1000;

    private final String mAppId;
    private final String mUniqueDeviceId;
    private final HttpURLConnectionFactory mHttpURLConnectionFactory;

    @Inject
    EventLoggerApi(Context context, HttpURLConnectionFactory httpURLConnectionFactory) {
        this(context.getResources().getString(R.string.app_id), AndroidUtils.getUniqueDeviceID(context), httpURLConnectionFactory);
    }

    @VisibleForTesting
    EventLoggerApi(String appId, String uniqueDeviceId, HttpURLConnectionFactory httpURLConnectionFactory) {
        mAppId = appId;
        mUniqueDeviceId = uniqueDeviceId;
        mHttpURLConnectionFactory = httpURLConnectionFactory;
    }

    /**
     * Submit play events to the server using the given urls
     *
     * @param urlPairs pair [id, url] of the event to push
     * @return  a list of successfully submitted ids
     */
    public String[] pushToRemote(List<Pair<Long, String>> urlPairs) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Pushing " + urlPairs.size() + " new tracking events");

        List<String> successes = new ArrayList<String>(urlPairs.size());
        HttpURLConnection connection = null;

        for (Pair<Long,String> urlPair : urlPairs){
            try {
                final String url = urlPair.second;
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "logging "+url);
                connection = mHttpURLConnectionFactory.create(url);
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.connect();

                final int response = connection.getResponseCode();

                if (response == HttpStatus.SC_OK) {
                    successes.add(String.valueOf(urlPair.first));
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
        Uri.Builder builder = Uri.parse(ENDPOINT).buildUpon();
        builder.appendPath(trackingData.getString(trackingData.getColumnIndex(PATH)));
        builder.appendQueryParameter("client_id", mAppId);
        builder.appendQueryParameter("anonymous_id", mUniqueDeviceId);

        String sourceInfoQueryString = trackingData.getString(trackingData.getColumnIndex(PARAMS));
        if (ScTextUtils.isNotBlank(sourceInfoQueryString)){
            return builder.toString() + "&" + sourceInfoQueryString;
        }
        return builder.toString();
    }
}
