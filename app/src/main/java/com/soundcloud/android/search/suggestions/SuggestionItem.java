package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

class SuggestionItem {

    enum Kind {
        SearchItem, TrackItem, UserItem
    }

    private final Kind kind;
    private final String query;

    protected SuggestionItem(Kind kind, String query) {
        this.kind = kind;
        this.query = query;
    }

    Urn getUrn() {
        return Urn.NOT_SET;
    }

    Kind getKind() {
        return kind;
    }

    String getQuery() {
        return query;
    }

    static SuggestionItem forSearch(String query) {
        return new SuggestionItem(Kind.SearchItem, query);
    }

    static SuggestionItem forUser(PropertySet source, String query) {
        return new SearchSuggestionItem(Kind.UserItem, source, query);
    }

    static SuggestionItem forTrack(PropertySet source, String query) {
        return new SearchSuggestionItem(Kind.TrackItem, source, query);
    }
}
