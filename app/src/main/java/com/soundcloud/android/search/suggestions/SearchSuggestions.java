package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

class SearchSuggestions<T extends SearchSuggestion> {

    public static <T extends SearchSuggestion> SearchSuggestions<T> empty() {
        return new SearchSuggestions<>(Collections.<T>emptyList(), Urn.NOT_SET);
    }

    private final List<T> collection;
    private final Urn queryUrn;

    protected SearchSuggestions(List<T> collection, Urn queryUrn) {
        this.collection = collection;
        this.queryUrn = queryUrn;
    }

    Urn getQueryUrn() {
        return queryUrn;
    }

    public List<T> getCollection() {
        return collection;
    }

    public static SearchSuggestions<Shortcut> fromShortcuts(List<Shortcut> shortcuts){
        return new SearchSuggestions<>(shortcuts, Urn.NOT_SET);
    }
}
