package com.soundcloud.android.events;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;

public class ForegroundEvent extends TrackingEvent {
    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_PAGE_URN = "page_urn";
    public static final String KEY_REFERRER = "referrer";
    public static final String KIND_OPEN = "open";

    public static ForegroundEvent open(Screen screen, Referrer referrer) {
        return new ForegroundEvent(KIND_OPEN)
                .put(KEY_PAGE_NAME, screen.get())
                .put(KEY_REFERRER, referrer.get());
    }

    public static ForegroundEvent open(Screen screen, Referrer referrer, Urn urn) {
        return ForegroundEvent.open(screen, referrer)
                .put(KEY_PAGE_URN, urn.toString());
    }

    private ForegroundEvent(String kind) {
        super(kind, System.currentTimeMillis());
    }

    @Override
    public ForegroundEvent put(String key, String value) {
        return (ForegroundEvent) super.put(key, value);
    }
}
