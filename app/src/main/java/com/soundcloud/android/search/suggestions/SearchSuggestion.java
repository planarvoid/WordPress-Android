package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;

import java.util.List;
import java.util.Map;

public interface SearchSuggestion {
    Urn getUrn();

    String getQuery();

    List<Map<String, Integer>> getHighlights();

    boolean isRemote();
}
