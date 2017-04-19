package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.ConnectionHelper;

import android.os.Looper;

import javax.inject.Inject;

class TrackingHandlerFactory {

    private final ConnectionHelper connectionHelper;
    private final TrackingStorage storage;
    private final TrackingApiFactory trackingApiFactory;

    @Inject
    TrackingHandlerFactory(ConnectionHelper connectionHelper,
                           TrackingStorage storage,
                           TrackingApiFactory trackingApiFactory) {
        this.connectionHelper = connectionHelper;
        this.storage = storage;
        this.trackingApiFactory = trackingApiFactory;
    }

    TrackingHandler create(Looper looper) {
        return new TrackingHandler(looper, connectionHelper, storage, trackingApiFactory);
    }
}
