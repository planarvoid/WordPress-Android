package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONNECTION_TYPE;

final class EventLoggerEventDataV1 extends EventLoggerEventData {

    public EventLoggerEventDataV1(String event, String version, String clientId, String anonymousId,
                                  String loggedInUserUrn, String timestamp, String connectionType) {
        super(event, version, clientId, anonymousId, loggedInUserUrn, timestamp);
        addToPayload(CONNECTION_TYPE, connectionType);
    }
}
