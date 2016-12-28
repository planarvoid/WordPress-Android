package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public abstract class SearchSuggestion {
    abstract Urn getUrn();

    abstract String getQuery();

    abstract Optional<SuggestionHighlight> getHighlights();

    abstract boolean isRemote();

    abstract Optional<String> getImageUrlTemplate();
}
