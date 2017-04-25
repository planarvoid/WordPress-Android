package com.soundcloud.android.analytics;

import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.utils.DeviceHelper;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Named;

class TrackingApiFactory {

    private static final int BATCH_SIZE = 100;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;
    private final String eventgatewayUrl;

    @Inject
    TrackingApiFactory(OkHttpClient httpClient,
                       DeviceHelper deviceHelper,
                       @Named(ApiModule.EVENTGATEWAY_BASE_URL) String eventgatewayUrl) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
        this.eventgatewayUrl = eventgatewayUrl;
    }

    TrackingApi create(String backend) {
        if (EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME.equals(backend)) {
            return new BatchTrackingApi(httpClient,
                                        deviceHelper,
                                        eventgatewayUrl,
                                        BATCH_SIZE);
        } else {
            return new SimpleTrackingApi(httpClient, deviceHelper);
        }
    }

}
