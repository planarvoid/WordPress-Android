package com.soundcloud.android.analytics.eventlogger;

enum Parameters {
    CLIENT_ID("client_id"),
    ANONYMOUS_ID("anonymous_id"),
    TIMESTAMP("ts"),
    ACTION("action"),
    DURATION("duration"),
    SOUND("sound"),
    USER("user"),
    CONTEXT("context"),
    TRIGGER("trigger"),
    SOURCE("source"),
    SOURCE_VERSION("source_version"),
    PLAYLIST_ID("set_id"),
    PLAYLIST_POSITION("set_position"),
    LATENCY("latency"),
    PROTOCOL("protocol"),
    PLAYER_TYPE("player_type"),
    TYPE("type"),
    HOST("host"),
    CONNECTION_TYPE("connection_type"),
    OS("os"),
    BITRATE("bitrate"),
    FORMAT("format"),
    URL("url"),
    ERROR_CODE("errorCode");

    private final String value;

    Parameters(String value) {
        this.value = value;
    }

    public String value(){
        return value;
    }
}
