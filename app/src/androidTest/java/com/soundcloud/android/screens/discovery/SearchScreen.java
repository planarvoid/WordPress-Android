package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class SearchScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;

    public SearchScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SearchResultsScreen doSearch(String query) {
        return actionBar().doSearch(query);
    }

    public SearchScreen setSearchQuery(String query) {
        actionBar().setSearchQuery(query);
        return this;
    }

    public String getSearchQuery() {
        return actionBar().getSearchQuery();
    }

    public SearchScreen dismissSearch() {
        actionBar().dismissSearch();
        return this;
    }

    public boolean hasSearchResults() {
        return testDriver.findOnScreenElement(With.id(android.R.id.list)).isOnScreen();
    }
}
