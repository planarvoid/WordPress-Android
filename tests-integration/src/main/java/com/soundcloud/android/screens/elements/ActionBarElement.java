package com.soundcloud.android.screens.elements;


import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.os.Build;
import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;

import java.util.List;

public class ActionBarElement extends Element {

    private static final int SEARCH_SELECTOR = R.id.action_search;

    public ActionBarElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return SEARCH_SELECTOR;
    }

    public void clickHomeButton() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            solo.clickOnActionBarHomeButton();
        } else {
            solo.findElement(With.id(R.id.up)).click();
        }
    }

    public PlaylistTagsScreen clickSearchButton() {
        waiter.waitForElement(SEARCH_SELECTOR);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            solo.clickOnActionBarItem(SEARCH_SELECTOR);
        } else {
            solo.findElement(With.id(SEARCH_SELECTOR)).click();
        }
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
        return searchInputField().getText().toString();
    }

    public void setSearchQuery(final String query) {
        searchInputField().typeText(query);
    }

    private ViewElement searchInputField() {
        waiter.waitForElement(AutoCompleteTextView.class);
        solo.getSolo().waitForView(AutoCompleteTextView.class);
        List<AutoCompleteTextView> views = solo.getSolo().getCurrentViews(AutoCompleteTextView.class);
        assertEquals("Expected to find just one search view", 1, views.size());
        return solo.wrap(views.get(0));
    }

    public PlaylistTagsScreen dismissSearch() {
        searchInputField().clearText();
        return new PlaylistTagsScreen(solo);
    }

}
