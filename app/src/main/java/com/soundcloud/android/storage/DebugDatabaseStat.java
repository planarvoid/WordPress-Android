package com.soundcloud.android.storage;

public class DebugDatabaseStat {

    private final String operation;
    private final long duration;

    public DebugDatabaseStat(String operation, long duration) {
        this.operation = operation;
        this.duration = duration;
    }

    public String operation() {
        return operation;
    }

    public long duration() {
        return duration;
    }
}
