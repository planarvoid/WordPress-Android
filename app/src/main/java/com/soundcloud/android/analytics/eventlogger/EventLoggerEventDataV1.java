package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONNECTION_TYPE;

final class EventLoggerEventDataV1 extends EventLoggerEventData {

    public EventLoggerEventDataV1(String event, String version, int clientId, String anonymousId,
                                  String loggedInUserUrn, long timestamp, String connectionType) {
        super(event, version, clientId, anonymousId, loggedInUserUrn, timestamp);
        addToPayload(CONNECTION_TYPE, connectionType);
    }

    @Override
    protected void addToPayload(String key, int value) {
        payload.put(key, value);
    }

    @Override
    protected void addToPayload(String key, long value) {
        payload.put(key, value);
    }
}
