package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.NetworkConnectionHelper;

import android.os.Looper;

import javax.inject.Inject;

class TrackingHandlerFactory {

    private final NetworkConnectionHelper networkConnectionHelper;
    private final TrackingStorage storage;
    private final TrackingApiFactory trackingApiFactory;

    @Inject
    TrackingHandlerFactory(NetworkConnectionHelper networkConnectionHelper,
                           TrackingStorage storage,
                           TrackingApiFactory trackingApiFactory) {
        this.networkConnectionHelper = networkConnectionHelper;
        this.storage = storage;
        this.trackingApiFactory = trackingApiFactory;
    }

    TrackingHandler create(Looper looper) {
        return new TrackingHandler(looper, networkConnectionHelper, storage, trackingApiFactory);
    }
}
