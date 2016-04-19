package com.soundcloud.android.search.suggestions;

class SearchSuggestionItem extends SuggestionItem {

    private final String query;

    SearchSuggestionItem(String query) {
        super(Kind.SearchItem);
        this.query = query;
    }

    String getQuery() {
        return query;
    }
}
