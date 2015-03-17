package com.soundcloud.android.analytics;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class TrackingApiFactory {

    private static final int READ_TIMEOUT = 5;
    private static final int CONNECT_TIMEOUT = 10;

    private static final int BATCH_SIZE = 100;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;

    @Inject
    TrackingApiFactory(OkHttpClient httpClient, DeviceHelper deviceHelper) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
    }

    TrackingApi create(String backend) {
        httpClient.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        if (EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME.equals(backend)) {
            return new BatchTrackingApi(httpClient, deviceHelper, EventLoggerAnalyticsProvider.BATCH_ENDPOINT, BATCH_SIZE);
        } else {
            return new SimpleTrackingApi(httpClient, deviceHelper);
        }
    }

}
