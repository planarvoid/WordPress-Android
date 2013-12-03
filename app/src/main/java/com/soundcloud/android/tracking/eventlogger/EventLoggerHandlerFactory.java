package com.soundcloud.android.tracking.eventlogger;

import android.content.Context;
import android.os.Looper;

import javax.inject.Inject;

public class EventLoggerHandlerFactory {

    private Context mContext;
    private PlayEventTrackingDbHelper mTrackingDbHelper;
    private PlayEventTrackingApi mTrackingApi;

    @Inject
    public EventLoggerHandlerFactory (Context context, PlayEventTrackingDbHelper playEventTrackingDbHelper, PlayEventTrackingApi trackingApi) {
        mContext = context;
        mTrackingDbHelper = playEventTrackingDbHelper;
        mTrackingApi = trackingApi;
    }

    public EventLoggerHandler create(Looper looper) {
        return new EventLoggerHandler(looper, mContext, mTrackingDbHelper, mTrackingApi);
    }
}
