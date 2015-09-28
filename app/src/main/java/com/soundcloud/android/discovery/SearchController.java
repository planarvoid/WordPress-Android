package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;

class SearchController {

    interface SearchCallback {
        void performTextSearch(Context context, String query);

        void launchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri);
    }

    private final SuggestionsAdapter suggestionsAdapter;

    private SearchCallback searchCallback;
    private AutoCompleteTextView searchView;

    @Inject
    SearchController(SuggestionsAdapter suggestionsAdapter) {
        this.suggestionsAdapter = suggestionsAdapter;
    }

    void bindSearchView(AutoCompleteTextView searchView, SearchCallback searchCallback) {
        this.searchView = checkNotNull(searchView);
        this.searchCallback = checkNotNull(searchCallback);
        initSearchView();
    }

    private void initSearchView() {
        searchView.setAdapter(suggestionsAdapter);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (suggestionsAdapter.isSearchItem(position)) {
                    searchCallback.performTextSearch(searchView.getContext(), searchView.getText().toString().trim());
                } else {
                    final SearchQuerySourceInfo searchQuerySourceInfo = getQuerySourceInfo(position);
                    final Uri itemUri = suggestionsAdapter.getItemIntentData(position);
                    searchCallback.launchSearchSuggestion(searchView.getContext(), suggestionsAdapter.getUrn(position),
                            searchQuerySourceInfo, itemUri);
                }
            }
        });
    }

    private SearchQuerySourceInfo getQuerySourceInfo(int position) {
        SearchQuerySourceInfo searchQuerySourceInfo = null;
        Urn queryUrn = suggestionsAdapter.getQueryUrn(position);
        if (!queryUrn.equals(Urn.NOT_SET)) {
            searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn,
                    suggestionsAdapter.getQueryPosition(position),
                    suggestionsAdapter.getUrn(position));
        }
        return searchQuerySourceInfo;
    }
}
