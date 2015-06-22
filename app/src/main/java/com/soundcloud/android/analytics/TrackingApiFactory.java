package com.soundcloud.android.analytics;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class TrackingApiFactory {

    private static final int READ_TIMEOUT = 5;
    private static final int CONNECT_TIMEOUT = 10;

    private static final int BATCH_SIZE = 100;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    @Inject
    TrackingApiFactory(OkHttpClient httpClient, DeviceHelper deviceHelper, Resources resources) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    TrackingApi create(String backend) {
        httpClient.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        if (EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME.equals(backend)) {
            return new BatchTrackingApi(httpClient, deviceHelper, resources.getString(R.string.eventgateway_url), BATCH_SIZE);
        } else {
            return new SimpleTrackingApi(httpClient, deviceHelper);
        }
    }

}
