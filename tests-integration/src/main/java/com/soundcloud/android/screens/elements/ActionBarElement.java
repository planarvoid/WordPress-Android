package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.WhoToFollowScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.EditTextElement;
import com.soundcloud.android.tests.viewelements.TextElement;
import com.soundcloud.android.tests.viewelements.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.content.res.Resources;
import android.os.Build;
import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;

public class ActionBarElement extends Element {

    private static final int SEARCH_SELECTOR = R.id.action_search;
    private static final int TITLE = Resources.getSystem().getIdentifier( "action_bar_title", "id", "android");
    private static final int CONTAINER = Resources.getSystem().getIdentifier( "action_bar_container", "id", "android");
    private final Han testDriver;

    public ActionBarElement(Han solo) {
        super(solo);
        testDriver = solo;
    }

    public String getTitle() {
        return title().getText();
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

    public ActivitiesScreen clickActivityOverflowButton() {
        testDriver.clickOnActionBarItem(R.id.action_activity);
        return new ActivitiesScreen(testDriver);
    }

    public WhoToFollowScreen clickWhoToFollowOverflowButton() {
        testDriver.clickOnActionBarItem(R.id.action_who_to_follow);
        return new WhoToFollowScreen(testDriver);
    }

    public SettingsScreen clickSettingsOverflowButton() {
        testDriver.clickOnActionBarItem(R.id.action_settings);
        return new SettingsScreen(testDriver);
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

    private TextElement title() {
        return new TextElement(actionBarContainer().findElement(With.id(TITLE)));
    }

    private ViewElement actionBarContainer() {
        return solo.findElement(With.id(CONTAINER));
    }

    private EditTextElement searchInputField() {
        waiter.waitForElement(AutoCompleteTextView.class);
        return new EditTextElement(solo.findElement(With.className(AutoCompleteTextView.class)));
    }

    public PlaylistTagsScreen dismissSearch() {
        searchInputField().clearText();
        return new PlaylistTagsScreen(solo);
    }

}
