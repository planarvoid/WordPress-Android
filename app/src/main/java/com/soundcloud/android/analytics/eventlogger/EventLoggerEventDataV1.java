package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.APP_VERSION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_ATTRIBUTES;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONNECTION_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.OVERFLOW_MENU;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TRIGGER;

import com.soundcloud.java.strings.Strings;

import java.util.HashMap;

final class EventLoggerEventDataV1 extends EventLoggerEventData {

    public EventLoggerEventDataV1(String event, String version, int clientId, String anonymousId,
                                  String loggedInUserUrn, long timestamp, String connectionType, String appVersion) {
        super(event, version, clientId, anonymousId, loggedInUserUrn, timestamp);
        addToPayload(CONNECTION_TYPE, connectionType);
        addToPayload(APP_VERSION, appVersion);
    }

    @Override
    protected void addToPayload(String key, boolean value) {
        payload.put(key, value);
    }

    @Override
    protected void addToPayload(String key, int value) {
        payload.put(key, value);
    }

    @Override
    protected void addToPayload(String key, long value) {
        payload.put(key, value);
    }

    @Override
    public EventLoggerEventData fromOverflowMenu(boolean fromOverflowMenu) {
        getClickAttributes().put(OVERFLOW_MENU, fromOverflowMenu);
        return this;
    }

    @Override
    public EventLoggerEventData clickSource(String source) {
        if (Strings.isNotBlank(source)) {
            getClickAttributes().put(SOURCE, source);
        }
        return this;
    }

    @Override
    public EventLoggerEventData clickSourceUrn(String sourceUrn) {
        if (Strings.isNotBlank(sourceUrn)) {
            getClickAttributes().put(SOURCE_URN, sourceUrn);
        }
        return this;
    }

    @Override
    public EventLoggerEventData clickTrigger(String trigger) {
        if (Strings.isNotBlank(trigger)) {
            getClickAttributes().put(TRIGGER, trigger);
        }
        return this;
    }

    private HashMap<String, Object> getClickAttributes() {
        if (!payload.containsKey(CLICK_ATTRIBUTES)) {
            payload.put(CLICK_ATTRIBUTES, new HashMap<String, Object>());
        }
        return (HashMap<String, Object>) payload.get(CLICK_ATTRIBUTES);
    }
}
