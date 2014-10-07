package com.soundcloud.android.events;

import com.soundcloud.android.analytics.Screen;

public final class ScreenEvent extends TrackingEvent {

    public static final String KEY_SCREEN = "screen";

    public static ScreenEvent create(Screen screen) {
        return new ScreenEvent(screen.get());
    }

    public static ScreenEvent create(String screen) {
        return new ScreenEvent(screen);
    }

    private ScreenEvent(String screen) {
        super(TrackingEvent.KIND_DEFAULT, System.currentTimeMillis());
        put(KEY_SCREEN, screen);
    }
}
