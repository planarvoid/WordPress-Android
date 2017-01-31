package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.search.TabbedSearchFragment;

import android.widget.TextView;

public class ToolBarElement extends Element {

    private static final int SEARCH_EDIT_TEXT = R.id.search_text;
    private static final int SEARCH_DISMISS_VIEW = R.id.search_close;
    private static final int CONTAINER = R.id.toolbar_id;

    public ToolBarElement(Han testDriver) {
        super(testDriver, With.id(CONTAINER));
    }

    public String getTitle() {
        return title().getText();
    }

    public ViewElement overflowButton() {
        return testDriver.findOnScreenElement(With.className(
                "android.support.v7.widget.ActionMenuPresenter$OverflowMenuButton"));
    }

    public SearchResultsScreen doSearch(String query) {
        setSearchQuery(query);
        testDriver.pressSoftKeyboardSearchButton();
        waiter.waitForActivity(SearchActivity.class);
        waiter.waitForFragmentByTag(TabbedSearchFragment.TAG);
        return new SearchResultsScreen(testDriver);
    }

    public ToolBarElement setSearchQuery(String query) {
        waiter.waitForActivity(SearchActivity.class);
        searchInputField().clearText();
        searchInputField().typeText(query);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public String getSearchQuery() {
        return searchInputField().getText();
    }

    public TextElement title() {
        return new TextElement(toolbar().findOnScreenElement(With.className(TextView.class)));
    }

    private ViewElement toolbar() {
        return testDriver.findOnScreenElement(With.id(CONTAINER));
    }

    private EditTextElement searchInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(SEARCH_EDIT_TEXT)));
    }

    public SearchScreen dismissSearch() {
        testDriver.findOnScreenElement(With.id(SEARCH_DISMISS_VIEW)).click();
        return new SearchScreen(testDriver);
    }
}
