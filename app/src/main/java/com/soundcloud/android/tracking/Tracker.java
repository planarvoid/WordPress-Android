package com.soundcloud.android.tracking;

public interface Tracker {
    void track(Event event, Object... args);
    void track(Class<?> klazz, Object... args);
}
