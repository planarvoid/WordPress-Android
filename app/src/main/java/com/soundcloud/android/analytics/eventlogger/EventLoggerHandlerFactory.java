package com.soundcloud.android.analytics.eventlogger;

import android.content.Context;
import android.os.Looper;

import javax.inject.Inject;

public class EventLoggerHandlerFactory {

    private final Context context;
    private final EventLoggerStorage storage;
    private final EventLoggerApi trackingApi;

    @Inject
    public EventLoggerHandlerFactory(Context context, EventLoggerStorage storage, EventLoggerApi trackingApi) {
        this.context = context;
        this.storage = storage;
        this.trackingApi = trackingApi;
    }

    public EventLoggerHandler create(Looper looper) {
        return new EventLoggerHandler(looper, context, storage, trackingApi);
    }
}
