package com.soundcloud.android.events;

public class BufferUnderrunEvent extends TrackingEvent {

    public static final String CONNECTION_TYPE = "connection_type";
    public static final String LOCKS_ACTIVE = "locks_active";

    public BufferUnderrunEvent(PlaybackPerformanceEvent.ConnectionType connectionType, boolean locksActive) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put(CONNECTION_TYPE, connectionType.getValue());
        put(LOCKS_ACTIVE, String.valueOf(locksActive));
    }


}
