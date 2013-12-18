package com.soundcloud.android.analytics.eventlogger;

public class EventLoggerParams {
    interface Keys {
        String ORIGIN_URL = "context";
        String TRIGGER = "trigger";
        String SOURCE = "source";
        String SOURCE_VERSION = "source_version";
        String SET_ID = "set_id";
        String SET_POSITION = "set_position";
    }

    interface Trigger {
        String AUTO = "auto";
        String MANUAL = "manual";
    }

    public interface Action {
        String PLAY = "play";
        String STOP = "stop";
    }
}
