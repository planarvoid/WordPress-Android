package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.search.suggestions.SuggestionsAdapter;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class SearchController {

    interface SearchCallback {
        void performTextSearch(Context context, String query);
    }

    private final SuggestionsAdapter suggestionsAdapter;

    private SearchCallback searchCallback;
    private AutoCompleteTextView searchView;

    @Inject
    SearchController(SuggestionsAdapter suggestionsAdapter) {
        this.suggestionsAdapter = suggestionsAdapter;
    }

    void bindSearchView(AutoCompleteTextView searchView, SearchCallback searchCallback) {
        checkNotNull(searchView);
        checkNotNull(searchCallback);
        this.searchView = searchView;
        this.searchCallback = searchCallback;
        initSearchView();
    }

    private void initSearchView() {
        searchView.setAdapter(suggestionsAdapter);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (suggestionsAdapter.isSearchItem(position)) {
                    if (searchCallback != null) {
                        searchCallback.performTextSearch(searchView.getContext(), searchView.getText().toString().trim());
                        searchView.setAdapter(null);
                    }
                }
            }
        });
    }
}
