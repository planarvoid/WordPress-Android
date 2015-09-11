package com.soundcloud.android.discovery;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class SearchController {

    private final Context context;
    private final PublicApi publicApi;

    private SuggestionsAdapter suggestionsAdapter;
    private SearchCallback searchCallback;
    private AutoCompleteTextView searchView;

    @Inject
    SearchController(Context context, PublicApi publicCloudAPI) {
        this.context = context;
        this.publicApi = publicCloudAPI;
    }

    public void bindSearchView(AutoCompleteTextView searchView, SearchCallback searchCallback) {
        Preconditions.checkNotNull(searchView);
        Preconditions.checkNotNull(searchCallback);
        this.searchView = searchView;
        this.searchCallback = searchCallback;
        initSearchView();
    }

    private void initSearchView() {
        suggestionsAdapter = new SuggestionsAdapter(context, publicApi, context.getContentResolver());
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
        String tag = query.replaceAll("^#+", ""); // Replaces the first occurrences of #
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
