package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.SlidingTabLayout;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class SearchResultsScreen extends Screen {
    private static final Class ACTIVITY = CombinedSearchActivity.class;

    private static final String ALL_TAB_TEXT = "ALL";
    private static final String TRACKS_TAB_TEXT = "TRACKS";
    private static final String PLAYLISTS_TAB_TEXT = "PLAYLISTS";
    private static final String PEOPLE_TAB_TEXT = "PEOPLE";

    private ViewPagerElement viewPager;

    public SearchResultsScreen(Han solo) {
        super(solo);
        viewPager = new ViewPagerElement(solo);
        waiter.waitForActivity(getActivity());
    }

    public PlayerScreen clickFirstTrackItem() {
        View itemView = getFirstResultsItemByClass(Track.class);
        solo.clickOnView(itemView);
        return new PlayerScreen(solo);
    }

    public PlaylistDetailsScreen clickFirstPlaylistItem() {
        View itemView = getFirstResultsItemByClass(Playlist.class);
        solo.clickOnView(itemView);
        return new PlaylistDetailsScreen(solo);
    }

    public ProfileScreen clickFirstUserItem() {
        View itemView = getFirstResultsItemByClass(User.class);
        solo.clickOnView(itemView);
        return new ProfileScreen(solo);
    }

    public MainScreen pressBack() {
        solo.goBack();
        // A supposition is made that the previous screen was the main screen
        return new MainScreen(solo);
    }

    public void touchAllTab() {
        touchTab(ALL_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void touchTracksTab() {
        touchTab(TRACKS_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void touchPlaylistsTab() {
        touchTab(PLAYLISTS_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void touchPeopleTab() {
        touchTab(PEOPLE_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        solo.scrollToBottom((ListView) viewPager.getCurrentPage(ListView.class));
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    private boolean touchTab(String tabText) {
        List<View> indicatorItems = getViewPagerIndicator().getChildAt(0).getTouchables();
        for(View view : indicatorItems){
            if(((TextView)view).getText().equals(tabText)){
                solo.performClick(view);
                return true;
            }
        }
        return false;
    }

    public String currentTabTitle(){
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        List<View> indicatorItems = getViewPagerIndicator().getChildAt(0).getTouchables();
        TextView selectedItem = (TextView) indicatorItems.get(getViewPager().getCurrentItem());
        return selectedItem.getText().toString();
    }

    private ViewPager getViewPager() {
        return (ViewPager)solo.getView(R.id.pager);
    }

    private SlidingTabLayout getViewPagerIndicator() {
        return (SlidingTabLayout)solo.getView(R.id.sliding_tabs);
    }

    public int getResultItemCount() {
        waiter.waitForItemCountToIncrease(resultsList().getAdapter(), 0);
        return resultsList().getAdapter().getCount();
    }

    private ListView resultsList() {
        return (ListView)viewPager.getCurrentPage(ListView.class);
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
        return waiter.waitForElement(R.id.search_results_container);
    }
}
