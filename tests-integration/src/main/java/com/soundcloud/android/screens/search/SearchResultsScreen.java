package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.SlidingTabLayout;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class SearchResultsScreen extends Screen {
    private static final Class ACTIVITY = CombinedSearchActivity.class;
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

    public LegacyPlayerScreen clickFirstTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        View itemView = getFirstResultsItemByClass(Track.class);
        solo.clickOnView(itemView);
        return new LegacyPlayerScreen(solo);
    }

    public PlaylistDetailsScreen clickFirstPlaylistItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        View itemView = getFirstResultsItemByClass(Playlist.class);
        solo.clickOnView(itemView);
        return new PlaylistDetailsScreen(solo);
    }

    public ProfileScreen clickFirstUserItem() {

        View itemView = getFirstResultsItemByClass(User.class);
        solo.clickOnView(itemView);
        return new ProfileScreen(solo);
    }

    public PlaylistTagsScreen pressBack() {
        solo.goBack();
        return new PlaylistTagsScreen(solo);
    }

    public void touchAllTab() {
        touchTab(ALL_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void touchTracksTab() {
        touchTab(TRACKS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void touchPlaylistsTab() {
        touchTab(PLAYLISTS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void touchPeopleTab() {
        touchTab(PEOPLE_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        solo.scrollToBottom((ListView) getViewPager().getCurrentPage(ListView.class));
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    private boolean touchTab(String tabText) {
        List<View> indicatorItems = getViewPagerIndicator().getChildAt(0).getTouchables();
        for (View view : indicatorItems) {
            if (((TextView) view).getText().equals(tabText)) {
                solo.performClick(view);
                return true;
            }
        }
        return false;
    }

    public String currentTabTitle() {
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        // toUppercase since SlidingTabLayout does the same
        return getViewPager().getCurrentTabText().toUpperCase();
    }

    private ViewPagerElement getViewPager() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ViewPagerElement(solo);
    }

    private SlidingTabLayout getViewPagerIndicator() {
        return (SlidingTabLayout) solo.getView(R.id.sliding_tabs);
    }

    public int getResultItemCount() {
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        return resultsList().getAdapter().getCount();
    }

    private ListView resultsList() {
        return (ListView) getViewPager().getCurrentPage(ListView.class);
    }

    private ViewPagerElement viewPagerElement() {
        return new ViewPagerElement(solo);
    }

    private View getFirstResultsItemByClass(Class itemClass) {
        ListAdapter adapter = resultsList().getAdapter();
        waiter.waitForItemCountToIncrease(adapter, 0);
        int numberOfItems = adapter.getCount();
        for (int i = 0; i < numberOfItems; i++) {
            if(itemClass.isInstance(adapter.getItem(i))) {
                return resultsList().getChildAt(i);
            }
        }
        return null;
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
