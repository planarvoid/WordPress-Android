package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class ToolBarElement extends Element {

    private static final int SEARCH_SELECTOR = R.id.action_search;
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

    public SearchResultsScreen doSearch(String query) {
        setSearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new SearchResultsScreen(testDriver);
    }

    public PlaylistResultsScreen doTagSearch(String query) {
        setSearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new PlaylistResultsScreen(testDriver);
    }

    public String getSearchQuery() {
        return searchInputField().getText();
    }

    public void setSearchQuery(final String query) {
        searchInputField().typeText(query);
    }

    public TextElement title() {
        return new TextElement(toolbar().findElement(With.className(TextView.class)));
    }

    private ViewElement toolbar() {
        return testDriver.findElement(With.id(CONTAINER));
    }

    private EditTextElement searchInputField() {
        waiter.waitForElement(AutoCompleteTextView.class);
        return new EditTextElement(testDriver.findElement(With.className(AutoCompleteTextView.class)));
    }

    public PlaylistTagsScreen dismissSearch() {
        searchInputField().clearText();
        return new PlaylistTagsScreen(testDriver);
    }

}
