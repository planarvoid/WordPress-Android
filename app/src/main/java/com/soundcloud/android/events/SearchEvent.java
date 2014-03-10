package com.soundcloud.android.events;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.storage.provider.Content;

import java.util.HashMap;
import java.util.Map;

public final class SearchEvent {

    public static final int SEARCH_SUGGESTION = 0;
    public static final int SEARCH_SUBMIT = 1;
    public static final int SEARCH_RESULTS = 2;

    private final int mKind;
    private final Map<String, String> mAttributes;

    private SearchEvent(int kind) {
        mKind = kind;
        mAttributes = new HashMap<String, String>();
    }

    public int getKind() {
        return mKind;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("Search Event with type id %s and %s", mKind, mAttributes.toString());
    }

    public static SearchEvent searchSuggestion(Content itemKind, boolean localResult) {
        return new SearchEvent(SEARCH_SUGGESTION)
                .putAttribute("type", itemKind.name().toLowerCase())
                .putAttribute("context", localResult ? "personal" : "global");
    }

    public static SearchEvent recentTagSearch(String tagQuery) {
        return new SearchEvent(SEARCH_SUBMIT)
                .putAttribute("type", "tag")
                .putAttribute("location", "recent_tags")
                .putAttribute("content", tagQuery);
    }

    public static SearchEvent popularTagSearch(String tagQuery) {
        return new SearchEvent(SEARCH_SUBMIT)
                .putAttribute("type", "tag")
                .putAttribute("location", "popular_tags")
                .putAttribute("content", tagQuery);
    }

    public static SearchEvent searchField(String query, boolean viaShortcut, boolean tagSearch) {
        return new SearchEvent(SEARCH_SUBMIT)
                .putAttribute("type", tagSearch ? "tag" : "normal")
                .putAttribute("location", viaShortcut ? "search_suggestion" : "search_field")
                .putAttribute("content", query);
    }

    public static SearchEvent tapTrackOnScreen(Screen screen) {
        return new SearchEvent(SEARCH_RESULTS)
                .putAttribute("type", "track")
                .putAttribute("context", eventAttributeFromScreen(screen));
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen) {
        return new SearchEvent(SEARCH_RESULTS)
                .putAttribute("type", "playlist")
                .putAttribute("context", eventAttributeFromScreen(screen));
    }

    public static SearchEvent tapUserOnScreen(Screen screen) {
        return new SearchEvent(SEARCH_RESULTS)
                .putAttribute("type", "user")
                .putAttribute("context", eventAttributeFromScreen(screen));
    }

    private static String eventAttributeFromScreen(Screen screen) {
        switch (screen) {
            case SEARCH_EVERYTHING:
                return "everything";
            case SEARCH_TRACKS:
                return "tracks";
            case SEARCH_PLAYLISTS:
                return "playlists";
            case SEARCH_USERS:
                return "people";
            case SEARCH_PLAYLIST_DISCO:
                return "tags";
            default:
                throw new IllegalStateException("Unexpected screen: " + screen);
        }
    }

    private SearchEvent putAttribute(String key, String value) {
        mAttributes.put(key, value);
        return this;
    }
}
