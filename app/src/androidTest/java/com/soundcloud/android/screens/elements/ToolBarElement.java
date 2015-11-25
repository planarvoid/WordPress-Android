package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.annotation.BrokenSearchTest;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.search.LegacySearchResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.search.TabbedSearchFragment;

import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class ToolBarElement extends Element {

    private static final int SEARCH_SELECTOR = R.id.action_search;
    private static final int SEARCH_EDIT_TEXT = R.id.search_text;
    private static final int SEARCH_DISMISS_VIEW = R.id.search_close;
    private static final int CONTAINER = R.id.toolbar_id;
    protected final Han testDriver;

    public ToolBarElement(Han solo) {
        super(solo, With.id(CONTAINER));
        testDriver = solo;
    }

    public String getTitle() {
        return title().getText();
    }

    public ViewElement overflowButton() {
        return testDriver.findElement(With.className("android.support.v7.widget.ActionMenuPresenter$OverflowMenuButton"));
    }

    public PlaylistTagsScreen clickSearchButton() {
        waiter.waitForElement(SEARCH_SELECTOR);
        solo.findElement(With.id(SEARCH_SELECTOR)).click();
        return new PlaylistTagsScreen(solo);
    }

    /**
     * Performs a legacy search. Will be removed when we get rid of
     * all tests marked with {@link BrokenSearchTest}
     *
     * @deprecated use {@link #doSearch(String)} instead.
     */
    @Deprecated
    public LegacySearchResultsScreen doLegacySearch(String query) {
        setLegacySearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new LegacySearchResultsScreen(testDriver);
    }

    public SearchResultsScreen doSearch(String query) {
        setSearchQuery(query);
        testDriver.sendKey(KeyEvent.KEYCODE_ENTER);
        waiter.waitForActivity(SearchActivity.class);
        waiter.waitForFragmentByTag(TabbedSearchFragment.TAG);
        return new SearchResultsScreen(testDriver);
    }

    public ToolBarElement setSearchQuery(String query) {
        waiter.waitForActivity(SearchActivity.class);
        searchInputField().typeText(query);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public String getSearchQuery() {
        return searchInputField().getText();
    }

    public String getLegacySearchQuery() {
        return legacySearchInputField().getText();
    }

    public void setLegacySearchQuery(final String query) {
        legacySearchInputField().typeText(query);
    }

    public TextElement title() {
        return new TextElement(toolbar().findElement(With.className(TextView.class)));
    }

    private ViewElement toolbar() {
        return testDriver.findElement(With.id(CONTAINER));
    }

    private EditTextElement searchInputField() {
        return new EditTextElement(testDriver.findElement(With.id(SEARCH_EDIT_TEXT)));
    }

    private EditTextElement legacySearchInputField() {
        waiter.waitForElement(AutoCompleteTextView.class);
        return new EditTextElement(testDriver.findElement(With.className(AutoCompleteTextView.class)));
    }

    public SearchResultsScreen dismissSearch() {
        testDriver.findElement(With.id(SEARCH_DISMISS_VIEW)).click();
        return new SearchResultsScreen(testDriver);
    }

    public PlaylistTagsScreen legacyDismissSearch() {
        legacySearchInputField().clearText();
        return new PlaylistTagsScreen(testDriver);
    }
}
