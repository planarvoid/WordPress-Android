package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.search.SearchActivity;

import android.widget.ListView;

public class SearchResultsScreen extends Screen {
    private static final Class ACTIVITY = SearchActivity.class;
    private static final String FRAGMENT = "tabbed_search";

    private static final String ALL_TAB_TEXT = "ALL";
    private static final String TRACKS_TAB_TEXT = "TRACKS";
    private static final String PLAYLISTS_TAB_TEXT = "PLAYLISTS";
    private static final String PEOPLE_TAB_TEXT = "PEOPLE";

    public SearchResultsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
        waiter.waitForFragmentByTag(FRAGMENT);
    }

    public VisualPlayerElement clickFirstTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public void clickFirstPlaylistItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(R.id.playlist_list_item)).click();
    }

    public ProfileScreen clickFirstUserItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(R.id.user_list_item)).click();
        return new ProfileScreen(testDriver);
    }

    public void pressBack() {
        testDriver.goBack();
    }

    public SearchResultsScreen touchTracksTab() {
        touchTab(TRACKS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public SearchResultsScreen touchPlaylistsTab() {
        touchTab(PLAYLISTS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public void touchPeopleTab() {
        touchTab(PEOPLE_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        testDriver.scrollToBottom((ListView) getViewPager().getCurrentPage(ListView.class));
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    public String currentTabTitle() {
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        // toUppercase since SlidingTabLayout does the same
        return getViewPager().getCurrentTabText().toUpperCase();
    }

    private ViewPagerElement getViewPager() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ViewPagerElement(testDriver);
    }

    public int getResultItemCount() {
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        return resultsList().getAdapter().getCount();
    }

    public Object getResultItem(int position) {
        return resultsList().getAdapter().getItem(position);
    }

    private ListView resultsList() {
        return (ListView) getViewPager().getCurrentPage(ListView.class);
    }

    private SlidingTabs tabs(){
        return testDriver.findElement(With.id(R.id.sliding_tabs)).toSlidingTabs();
    }
    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }
}
