package com.soundcloud.android.discovery;

import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.strings.Strings;

import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class SearchController {

    private final SuggestionsAdapter suggestionsAdapter;

    private SearchCallback searchCallback;
    private AutoCompleteTextView searchView;

    @Inject
    SearchController(SuggestionsAdapter suggestionsAdapter) {
        this.suggestionsAdapter = suggestionsAdapter;
    }

    public void bindSearchView(AutoCompleteTextView searchView, SearchCallback searchCallback) {
        Preconditions.checkNotNull(searchView);
        Preconditions.checkNotNull(searchCallback);
        this.searchView = searchView;
        this.searchCallback = searchCallback;
        initSearchView();
    }

    private void initSearchView() {
        searchView.setAdapter(suggestionsAdapter);
    }

    private void performSearch(final String query) {
        final String trimmedQuery = query.trim();
        final boolean tagSearch = trimmedQuery.startsWith("#");

        if (tagSearch) {
            performTagSearch(trimmedQuery);
        } else {
            searchCallback.performTextSearch(trimmedQuery);
        }
    }

    private void performTagSearch(final String query) {
        String tag = query.replaceAll("^#+", "");
        if (!Strings.isNullOrEmpty(tag)) {
            searchCallback.performTagSearch(tag);
        }
    }

    public interface SearchCallback {
        void performTextSearch(String query);

        void performTagSearch(String tag);

        void exitSearchMode();
    }
}
