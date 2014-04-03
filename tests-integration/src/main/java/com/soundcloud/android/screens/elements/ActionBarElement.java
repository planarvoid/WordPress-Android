package com.soundcloud.android.screens.elements;


import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;

import java.util.List;

public class ActionBarElement {
    protected Han solo;
    protected Waiter waiter;

    private static final int SEARCH_SELECTOR = R.id.action_search;

    public ActionBarElement(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void clickHomeButton() {
        solo.clickOnActionBarHomeButton();
    }

    public PlaylistTagsScreen clickSearchButton() {
        waiter.waitForElement(SEARCH_SELECTOR);
        solo.clickOnActionBarItem(SEARCH_SELECTOR);
        return new PlaylistTagsScreen(solo);
    }

    public SearchResultsScreen doSearch(String query) {
        setSearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new SearchResultsScreen(solo);
    }

    public PlaylistResultsScreen doTagSearch(String query) {
        setSearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new PlaylistResultsScreen(solo);
    }

    public String getSearchQuery() {
        return getSearchView().getText().toString();
    }

    public void setSearchQuery(final String query) {
        solo.typeText(getSearchView(), query);
    }

    private AutoCompleteTextView getSearchView() {
        waiter.waitForElement(AutoCompleteTextView.class);
        solo.getSolo().waitForView(AutoCompleteTextView.class);
        List<AutoCompleteTextView> views = solo.getSolo().getCurrentViews(AutoCompleteTextView.class);
        assertEquals("Expected to find just one search view", 1, views.size());
        return views.get(0);
    }

    public PlaylistTagsScreen dismissSearch() {
        solo.clearEditText(getSearchView());
        return new PlaylistTagsScreen(solo);
    }

}
