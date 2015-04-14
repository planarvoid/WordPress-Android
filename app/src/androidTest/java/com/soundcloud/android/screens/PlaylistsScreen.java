package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.ViewPagerElement;

import android.widget.ListView;

public class PlaylistsScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public PlaylistsScreen(Han solo) {
        super(solo);
    }

    public PlaylistItemElement get(int index) {
        final ListElement listElement = playlistsListOnCurrentPage();
        return new PlaylistItemElement(testDriver, listElement.getItemAt(index));
    }

    public boolean hasLikes() {
        return !emptyView().isVisible() && playlistsListOnCurrentPage().getItemCount() > 0;
    }

    public PlaylistDetailsScreen clickPlaylistAt(int index) {
        playlistsList().getItemAt(index).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistDetailsScreen clickPlaylistOnCurrentPageAt(int index) {
        playlistsListOnCurrentPage().getItemAt(index).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    //TODO: Wait for the tab to be displayed
    public PlaylistsScreen touchLikedPlaylistsTab() {
        touchTab(testDriver.getString(R.string.liked_playlists_tab).toUpperCase());
        waiter.waitTwoSeconds();
        return this;
    }

    //TODO: Wait for the tab to be displayed
    public PlaylistsScreen touchPostedPlaylistsTab() {
        touchTab(testDriver.getString(R.string.your_playlists_tab).toUpperCase());
        waiter.waitTwoSeconds();
        return this;
    }

    public int getLoadedTrackCount(){
        waiter.waitForContentAndRetryIfLoadingFailed();
        return playlistsList().getAdapter().getCount();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        playlistsList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    private ViewPagerElement getViewPager() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ViewPagerElement(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private ListElement playlistsList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }

    private ListElement playlistsListOnCurrentPage() {
        return testDriver.findElement(With.className(ListView.class)).toListView();
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    private SlidingTabs tabs(){
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.sliding_tabs)).toSlidingTabs();
    }
}
