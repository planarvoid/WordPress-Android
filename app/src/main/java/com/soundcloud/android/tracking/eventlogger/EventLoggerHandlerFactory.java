package com.soundcloud.android.tracking.eventlogger;

import android.content.Context;
import android.os.Looper;

import javax.inject.Inject;

public class EventLoggerHandlerFactory {

    private Context mContext;
    private EventLoggerDbHelper mTrackingDbHelper;
    private EventLoggerApi mTrackingApi;

    @Inject
    public EventLoggerHandlerFactory (Context context, EventLoggerDbHelper eventLoggerDbHelper, EventLoggerApi trackingApi) {
        mContext = context;
        mTrackingDbHelper = eventLoggerDbHelper;
        mTrackingApi = trackingApi;
    }

    public EventLoggerHandler create(Looper looper) {
        return new EventLoggerHandler(looper, mContext, mTrackingDbHelper, mTrackingApi);
    }
}
