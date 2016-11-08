package com.soundcloud.android.analytics;

import static com.soundcloud.android.analytics.AnalyticsModule.TRACKING_HTTP_CLIENT;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.utils.DeviceHelper;
import okhttp3.OkHttpClient;

import android.content.res.Resources;

import javax.inject.Inject;
import javax.inject.Named;

class TrackingApiFactory {

    private static final int BATCH_SIZE = 100;

    private final OkHttpClient httpClient;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    @Inject
    TrackingApiFactory(@Named(TRACKING_HTTP_CLIENT) OkHttpClient httpClient,
                       DeviceHelper deviceHelper, Resources resources) {
        this.httpClient = httpClient;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    TrackingApi create(String backend) {
        if (EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME.equals(backend)) {
            return new BatchTrackingApi(httpClient,
                                        deviceHelper,
                                        resources.getString(R.string.eventgateway_url),
                                        BATCH_SIZE);
        } else {
            return new SimpleTrackingApi(httpClient, deviceHelper);
        }
    }

}
