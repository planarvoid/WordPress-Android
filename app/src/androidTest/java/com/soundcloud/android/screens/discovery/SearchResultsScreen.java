package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.SearchTabs;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.support.v7.widget.RecyclerView;

public class SearchResultsScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;
    private static final String FRAGMENT = "tabbed_search";

    private final SearchTabs searchTabs;

    public SearchResultsScreen(Han solo) {
        super(solo);
        this.searchTabs = new SearchTabs(solo);
        waiter.waitForFragmentByTag(FRAGMENT);
    }

    public VisualPlayerElement clickFirstTrackItem() {
        testDriver.findElement(With.id(R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public PlaylistDetailsScreen clickFirstPlaylistItem() {
        testDriver.findElement(With.id(R.id.playlist_list_item)).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public ProfileScreen clickFirstUserItem() {
        testDriver.findElement(With.id(R.id.user_list_item)).click();
        return new ProfileScreen(testDriver);
    }

    public UserItemElement getFirstUser() {
        scrollListToItem(With.id(com.soundcloud.android.R.id.user_list_item));
        return getUsers().get(0);
    }

    public int getResultItemCount() {
        final RecyclerViewElement recyclerViewElement = resultsList();
        waiter.waitForItemCountToIncrease(recyclerViewElement.getAdapter(), 0);
        return recyclerViewElement.getItemCount();
    }

    public String currentTabTitle() {
        return getViewPager().getCurrentTabText();
    }

    public SearchResultsScreen scrollToBottomOfTracksListAndLoadMoreItems() {
        resultsList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public SearchResultsScreen goToAllTab() {
        return searchTabs.goToAllTab();
    }

    public SearchResultsScreen goToTracksTab() {
        return searchTabs.goToTracksTab();
    }

    public SearchResultsScreen goToPlaylistsTab() {
        return searchTabs.goToPlaylistsTab();
    }

    public SearchResultsScreen goToPeopleTab() {
        return searchTabs.goToPeopleTab();
    }

    private ViewPagerElement getViewPager() {
        return new ViewPagerElement(testDriver);
    }

    private RecyclerViewElement resultsList() {
        return testDriver.findElement(With.className(RecyclerView.class)).toRecyclerView();
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
