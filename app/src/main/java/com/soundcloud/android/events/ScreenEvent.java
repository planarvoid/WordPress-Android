package com.soundcloud.android.events;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.explore.ExploreGenre;
import com.soundcloud.java.strings.Strings;

public final class ScreenEvent extends TrackingEvent {

    public static final String KEY_SCREEN = "screen";
    public static final String KEY_GENRE = "genre";
    public static final String KEY_QUERY_URN = "query_urn";

    public static ScreenEvent create(Screen screen) {
        return new ScreenEvent(screen.get());
    }

    public static ScreenEvent create(String screen) {
        return new ScreenEvent(screen);
    }

    public static ScreenEvent create(String screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return new ScreenEvent(screen).put(KEY_QUERY_URN, searchQuerySourceInfo.getQueryUrn().toString());
    }

    public static ScreenEvent create(String screen, ExploreGenre genre) {
        return (ScreenEvent) new ScreenEvent(screen).put(KEY_GENRE, genre.getTitle());
    }

    private ScreenEvent(String screenTag) {
        super(TrackingEvent.KIND_DEFAULT, System.currentTimeMillis());
        put(KEY_SCREEN, screenTag);
    }

    public String getScreenTag() {
        return get(KEY_SCREEN);
    }

    @Override
    public String toString() {
        return "user entered " + getScreenTag();
    }

    public String getQueryUrn() {
        if (getAttributes().containsKey(KEY_QUERY_URN)) {
            return get(KEY_QUERY_URN);
        }
        return Strings.EMPTY;
    }
}
