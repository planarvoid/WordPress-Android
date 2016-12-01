package com.soundcloud.android.events;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.explore.ExploreGenre;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.strings.Strings;

public final class ScreenEvent extends LegacyTrackingEvent {

    public static final String KIND = "screen";
    public static final String KEY_SCREEN = "screen";
    public static final String KEY_GENRE = "genre";
    public static final String KEY_QUERY_URN = "query_urn";
    public static final String KEY_PAGE_URN = "page_urn";

    public static ScreenEvent create(Screen screen) {
        return new ScreenEvent(screen.get());
    }

    public static ScreenEvent create(String screen) {
        return new ScreenEvent(screen);
    }

    public static ScreenEvent create(String screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return ScreenEvent.create(screen, searchQuerySourceInfo.getQueryUrn());
    }
    public static ScreenEvent create(String screen, Urn queryUrn) {
        return new ScreenEvent(screen).put(KEY_QUERY_URN, queryUrn.toString());
    }

    public static ScreenEvent create(String screen, ExploreGenre genre) {
        return (ScreenEvent) new ScreenEvent(screen).put(KEY_GENRE, genre.getTitle());
    }

    public static ScreenEvent create(Screen screen, Urn pageUrn) {
        return (ScreenEvent) new ScreenEvent(screen.get()).put(KEY_PAGE_URN, pageUrn.toString());
    }

    private ScreenEvent(String screenTag) {
        super(KIND);

        put(KEY_SCREEN, screenTag);
    }

    public String getScreenTag() {
        return get(KEY_SCREEN);
    }

    public String getQueryUrn() {
        return getAttributes().containsKey(KEY_QUERY_URN) ? get(KEY_QUERY_URN) : Strings.EMPTY;
    }

    public String getPageUrn() {
        return getAttributes().containsKey(KEY_PAGE_URN) ? get(KEY_PAGE_URN) : Strings.EMPTY;
    }

    @Override
    public String toString() {
        return "user entered " + getScreenTag();
    }
}
