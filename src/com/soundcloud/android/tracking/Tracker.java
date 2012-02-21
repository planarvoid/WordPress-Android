package com.soundcloud.android.tracking;

public interface Tracker {

    void track(Page page, Object... args);
    void track(Click click, Object... args);
    void track(Class<?> klazz, Object... args);
}
