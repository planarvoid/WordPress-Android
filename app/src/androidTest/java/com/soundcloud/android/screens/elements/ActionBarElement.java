package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.SettingsScreen;
import com.soundcloud.android.screens.WhoToFollowScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;

import android.os.Build;
import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class ActionBarElement extends Element {

    private static final int SEARCH_SELECTOR = R.id.action_search;
    private static final int CONTAINER = R.id.action_bar_container;
    protected final Han testDriver;

    public ActionBarElement(Han solo) {
        super(solo);
        testDriver = solo;
    }

    public String getTitle() {
        return title().getText();
    }

    @Override
    protected int getRootViewId() {
        return R.id.action_bar;
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
        solo.findElement(With.id(SEARCH_SELECTOR)).click();
        return new PlaylistTagsScreen(solo);
    }

    public ActivitiesScreen clickActivityOverflowButton() {
        clickOverflowButton("Activity");
        return new ActivitiesScreen(testDriver);
    }

    public WhoToFollowScreen clickWhoToFollowOverflowButton() {
        clickOverflowButton("Who to follow");
        return new WhoToFollowScreen(testDriver);
    }

    public SettingsScreen clickSettingsOverflowButton() {
        clickOverflowButton("Settings");
        return new SettingsScreen(testDriver);
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
        return searchInputField().getText().toString();
    }

    public void setSearchQuery(final String query) {
        searchInputField().typeText(query);
    }

    private void clickOverflowButton(String buttonText) {
        testDriver.findElement(With.className("android.support.v7.widget.ActionMenuPresenter$OverflowMenuButton")).click();
        testDriver.findElement(With.text(buttonText)).click();
    }

    private TextElement title() {
        return new TextElement(actionBarContainer().findElement(With.id(R.id.action_bar)).findElement(With.className(TextView.class)));
    }

    private ViewElement actionBarContainer() {
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
