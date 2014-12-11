package com.soundcloud.android.events;

public final class BufferUnderrunEvent extends TrackingEvent {

    public static final String CONNECTION_TYPE = "connection_type";
    public static final String LOCKS_ACTIVE = "locks_active";
    private static final String PLAYER_TYPE = "player_type";

    public BufferUnderrunEvent(ConnectionType connectionType, String playerType, Boolean locksActive) {
        this(connectionType.getValue(), playerType, String.valueOf(locksActive));
    }

    public BufferUnderrunEvent(String connectionType, String playerType, String locksActive) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put(CONNECTION_TYPE, connectionType);
        put(PLAYER_TYPE, playerType);
        put(LOCKS_ACTIVE, locksActive);
    }
}
