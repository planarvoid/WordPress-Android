package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.NetworkConnectionHelper;

import android.os.Looper;

import javax.inject.Inject;

class TrackingHandlerFactory {

    private final NetworkConnectionHelper networkConnectionHelper;
    private final TrackingStorage storage;
    private final TrackingApi trackingApi;

    @Inject
    TrackingHandlerFactory(NetworkConnectionHelper networkConnectionHelper, TrackingStorage storage, TrackingApi trackingApi) {
        this.networkConnectionHelper = networkConnectionHelper;
        this.storage = storage;
        this.trackingApi = trackingApi;
    }

    TrackingHandler create(Looper looper) {
        return new TrackingHandler(looper, networkConnectionHelper, storage, trackingApi);
    }
}
