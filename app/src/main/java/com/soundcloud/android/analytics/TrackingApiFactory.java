package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class TrackingApiFactory {

    private static final int READ_TIMEOUT = 5;
    private static final int CONNECT_TIMEOUT = 10;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;

    @Inject
    TrackingApiFactory(OkHttpClient httpClient, DeviceHelper deviceHelper) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
    }

    TrackingApi create(String backend){
        httpClient.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        httpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        return new SimpleTrackingApi(httpClient, deviceHelper);
    }

}
