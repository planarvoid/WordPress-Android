package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.R;

import android.widget.AbsListView;
import android.widget.ListView;

public class PlaylistsScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public PlaylistsScreen(Han solo) {
        super(solo);
    }

    public PlaylistDetailsScreen clickPlaylistAt(int index) {
        playlistsList().getItemAt(index).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistDetailsScreen clickPlaylist(With matcher) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.scrollToBottom(getCurrentListView());
        testDriver.findElement(matcher).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public void touchLikedPlaylistsTab() {
        touchTab(testDriver.getString(R.string.liked_playlists_tab));
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
        return new ListElement(getViewPager().getCurrentPage(ListView.class), testDriver.getSolo());
    }

    private AbsListView getCurrentListView() {
        return (AbsListView) getViewPager().getCurrentPage(ListView.class);
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    private SlidingTabs tabs(){
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.sliding_tabs)).toSlidingTabs();
    }
}
