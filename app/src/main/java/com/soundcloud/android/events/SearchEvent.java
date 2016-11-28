package com.soundcloud.android.events;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

public final class SearchEvent extends TrackingEvent {

    public static final String KEY_TYPE = "type";
    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_CLICK_NAME = "click_name";
    public static final String KEY_CLICK_OBJECT = "click_object";
    public static final String KEY_CONTEXT = "context";
    public static final String KEY_QUERY_URN = "query_urn";
    public static final String KEY_QUERY_POSITION = "query_position";
    public static final String KEY_QUERY = "search_query";
    public static final String KEY_CONTENT = "content";

    private static final String KEY_LOCATION = "location";

    private static final String TYPE_TAG = "tag";
    private static final String TYPE_NORMAL = "normal";
    private static final String TYPE_TRACK = "track";
    private static final String TYPE_PLAYLIST = "playlist";
    private static final String TYPE_USER = "user";

    public static final String CLICK_NAME_ITEM_NAVIGATION = "item_navigation";

    private static final String CONTEXT_PERSONAL = "personal";
    private static final String CONTEXT_GLOBAL = "global";
    private static final String CONTEXT_EVERYTHING = "everything";
    private static final String CONTEXT_TRACKS = "tracks";
    private static final String CONTEXT_ALBUMS = "albums";
    private static final String CONTEXT_PLAYLISTS = "playlists";
    private static final String CONTEXT_PEOPLE = "people";
    private static final String CONTEXT_TAGS = "tags";
    private static final String CONTEXT_PREMIUM = "premium";

    private static final String LOCATION_RECENT_TAGS = "recent_tags";
    private static final String LOCATION_POPULAR_TAGS = "popular_tags";
    private static final String LOCATION_SUGGESTION = "search_suggestion";
    private static final String LOCATION_FIELD = "search_field";

    public static final String KIND_SUGGESTION = "suggestion";
    public static final String KIND_LOCAL_SUGGESTION = "local_suggestion";
    public static final String KIND_SUBMIT = "submit";
    public static final String KIND_FORMULATION_INIT = "search_formulation_init";
    public static final String KIND_FORMULATION_END = "search_formulation_end";
    public static final String KIND_RESULTS = "results";
    public static final String CLICK_NAME_SEARCH = "search";
    public static final String CLICK_FORMULATION_INIT = "search_formulation_init";
    public static final String CLICK_FORMULATION_END = "search_formulation_end";

    public static SearchEvent searchSuggestion(Urn urn,
                                               boolean localResult,
                                               SearchQuerySourceInfo searchQuerySourceInfo) {
        return new SearchEvent(KIND_SUGGESTION)
                .<SearchEvent>put(KEY_PAGE_NAME, Screen.SEARCH_SUGGESTIONS.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION)
                .<SearchEvent>put(KEY_TYPE, urn.isUser() ? TYPE_USER : TYPE_TRACK)
                .<SearchEvent>put(KEY_CONTEXT, localResult ? CONTEXT_PERSONAL : CONTEXT_GLOBAL)
                .addSearchQuerySourceInfo(searchQuerySourceInfo);
    }

    public static SearchEvent recentTagSearch(String tagQuery) {
        return new SearchEvent(KIND_SUBMIT)
                .put(KEY_TYPE, TYPE_TAG)
                .put(KEY_LOCATION, LOCATION_RECENT_TAGS)
                .put(KEY_CONTENT, tagQuery);
    }

    public static SearchEvent popularTagSearch(String tagQuery) {
        return new SearchEvent(KIND_SUBMIT)
                .put(KEY_TYPE, TYPE_TAG)
                .put(KEY_LOCATION, LOCATION_POPULAR_TAGS)
                .put(KEY_CONTENT, tagQuery);
    }

    public static SearchEvent searchField(String query, boolean viaShortcut, boolean tagSearch) {
        return new SearchEvent(KIND_SUBMIT)
                .put(KEY_TYPE, tagSearch ? TYPE_TAG : TYPE_NORMAL)
                .put(KEY_LOCATION, viaShortcut ? LOCATION_SUGGESTION : LOCATION_FIELD)
                .put(KEY_CLICK_NAME, CLICK_NAME_SEARCH)
                .put(KEY_CONTENT, query);
    }

    public static SearchEvent tapTrackOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return new SearchEvent(KIND_RESULTS)
                .<SearchEvent>put(KEY_PAGE_NAME, screen.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION)
                .<SearchEvent>put(KEY_TYPE, TYPE_TRACK)
                .<SearchEvent>put(KEY_CONTEXT, eventAttributeFromScreen(screen))
                .addSearchQuerySourceInfo(searchQuerySourceInfo);
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen) {
        return tapPlaylistOnScreen(screen, null);
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return new SearchEvent(KIND_RESULTS)
                .<SearchEvent>put(KEY_PAGE_NAME, screen.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION)
                .<SearchEvent>put(KEY_TYPE, TYPE_PLAYLIST)
                .<SearchEvent>put(KEY_CONTEXT, eventAttributeFromScreen(screen))
                .addSearchQuerySourceInfo(searchQuerySourceInfo);
    }

    public static SearchEvent tapUserOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return new SearchEvent(KIND_RESULTS)
                .<SearchEvent>put(KEY_PAGE_NAME, screen.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION)
                .<SearchEvent>put(KEY_TYPE, TYPE_USER)
                .<SearchEvent>put(KEY_CONTEXT, eventAttributeFromScreen(screen))
                .addSearchQuerySourceInfo(searchQuerySourceInfo);
    }

    public static SearchEvent tapLocalSuggestionOnScreen(Screen screen, Urn itemUrn, String query, int clickPosition) {
        return new SearchEvent(KIND_LOCAL_SUGGESTION)
                .<SearchEvent>put(KEY_PAGE_NAME, screen.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION)
                .<SearchEvent>put(KEY_TYPE, TYPE_USER)
                .<SearchEvent>put(KEY_CONTEXT, eventAttributeFromScreen(screen))
                .addQuery(query)
                .addClickObject(itemUrn)
                .addClickPosition(clickPosition);
    }

    public static SearchEvent searchStart(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return new SearchEvent(KIND_SUBMIT)
                .<SearchEvent>put(KEY_PAGE_NAME, screen.get())
                .<SearchEvent>put(KEY_CLICK_NAME, CLICK_NAME_SEARCH)
                .addSearchQuerySourceInfo(searchQuerySourceInfo);
    }

    public static SearchEvent searchFormulationEnd(Screen screen,
                                                   String query,
                                                   Optional<Urn> queryUrn,
                                                   Optional<Integer> queryPosition) {

        return new SearchEvent(KIND_FORMULATION_END)
                .addQuery(query)
                .put(KEY_PAGE_NAME, screen.get())
                .put(KEY_CLICK_NAME, CLICK_FORMULATION_END)
                .put(KEY_QUERY_URN, queryUrn)
                .put(KEY_QUERY_POSITION, queryPosition);
    }

    public Optional<Integer> queryPosition() {
        final String queryPosition = get(KEY_QUERY_POSITION);
        return queryPosition != null ? Optional.of(Integer.valueOf(queryPosition)) : Optional.<Integer>absent();
    }

    public static SearchEvent searchFormulationInit(Screen screen, String query) {
        return new SearchEvent(KIND_FORMULATION_INIT)
                .addQuery(query)
                .put(KEY_PAGE_NAME, screen.get())
                .put(KEY_CLICK_NAME, CLICK_FORMULATION_INIT);
    }

    public Optional<String> queryUrn() {
        return Optional.fromNullable(get(KEY_QUERY_URN));
    }

    private SearchEvent addSearchQuerySourceInfo(SearchQuerySourceInfo searchQuerySourceInfo) {
        if (searchQuerySourceInfo != null) {
            put(KEY_QUERY_URN, searchQuerySourceInfo.getQueryUrn().toString());

            addClickPosition(searchQuerySourceInfo.getClickPosition());

            addClickObject(searchQuerySourceInfo.getClickUrn());
        }
        return this;
    }

    private SearchEvent addClickObject(Urn itemUrn) {
        if (itemUrn != null) {
            put(KEY_CLICK_OBJECT, itemUrn.toString());
        }
        return this;
    }

    private SearchEvent addClickPosition(Integer currentPosition) {
        if (currentPosition >= 0) {
            put(KEY_QUERY_POSITION, currentPosition.toString());
        }
        return this;
    }

    private SearchEvent addQuery(String query) {
        put(KEY_QUERY, Strings.isNullOrEmpty(query) ? Optional.<String>absent() : Optional.of(query));
        return this;
    }

    private SearchEvent(String kind) {
        super(kind);
    }

    @Override
    public String toString() {
        return String.format("Search Event with type id %s and %s", kind, attributes.toString());
    }

    private static String eventAttributeFromScreen(Screen screen) {
        switch (screen) {
            case SEARCH_EVERYTHING:
                return CONTEXT_EVERYTHING;
            case SEARCH_TRACKS:
                return CONTEXT_TRACKS;
            case SEARCH_ALBUMS:
                return CONTEXT_ALBUMS;
            case SEARCH_PLAYLISTS:
                return CONTEXT_PLAYLISTS;
            case SEARCH_USERS:
                return CONTEXT_PEOPLE;
            case SEARCH_PLAYLIST_DISCO:
                return CONTEXT_TAGS;
            case SEARCH_PREMIUM_CONTENT:
                return CONTEXT_PREMIUM;
            default:
                throw new IllegalStateException("Unexpected screen: " + screen);
        }
    }

}
