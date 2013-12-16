package com.soundcloud.android.analytics.eventlogger;

import android.content.Context;
import android.os.Looper;

import javax.inject.Inject;

public class EventLoggerHandlerFactory {

    private Context mContext;
    private EventLoggerStorage mTrackingDbHelper;
    private EventLoggerApi mTrackingApi;

    @Inject
    public EventLoggerHandlerFactory (Context context, EventLoggerStorage eventLoggerStorage, EventLoggerApi trackingApi) {
        mContext = context;
        mTrackingDbHelper = eventLoggerStorage;
        mTrackingApi = trackingApi;
    }

    public EventLoggerHandler create(Looper looper) {
        return new EventLoggerHandler(looper, mContext, mTrackingDbHelper, mTrackingApi);
    }
}
