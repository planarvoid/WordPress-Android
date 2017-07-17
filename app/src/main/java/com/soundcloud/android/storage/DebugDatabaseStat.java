package com.soundcloud.android.storage;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DebugDatabaseStat {

    abstract String operation();
    abstract long duration();

    public static DebugDatabaseStat create(String operation, long duration) {
        return new AutoValue_DebugDatabaseStat(operation, duration);
    }
}
