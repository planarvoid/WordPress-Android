package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Waiter;
import com.viewpagerindicator.FixedWeightTabPageIndicator;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class ExploreScreen extends Screen {

    private static final String GENRES_TAB_TEXT = "GENRES";
    private static final String TRENDING_AUDIO_TAB_TEXT = "TRENDING AUDIO";
    private static final String TRENDING_MUSIC_TAB_TEXT = "TRENDING MUSIC";
    private Waiter waiter;
    private InstrumentationTestCase testCase;
    private MenuScreen menuScreen;

    public ExploreScreen(ActivityInstrumentationTestCase2 testCase) {
        super(testCase);
        this.waiter = new Waiter(solo);
        this.menuScreen = new MenuScreen(solo);
        this.testCase = testCase;
    }

    public void openExploreFromMenu(){
        menuScreen.openExplore();
        assertEquals("Could not find Trending Music after opening explore", TRENDING_MUSIC_TAB_TEXT, currentTabTitle());

    }

    public void touchGenresTab() {
        assertTrue("Could not touch the genres tab", touchTab(GENRES_TAB_TEXT));
        solo.waitForViewId(R.id.suggested_tracks_categories_list, 2000);
        assertEquals("Could not get to genres section", GENRES_TAB_TEXT, currentTabTitle());
    }

    public void clickElectronicGenre() {
        solo.clickOnText("Electronic");
        waiter.waitForListContent();
    }

    public void touchTrendingAudioTab() {
        assertTrue("Could not touch the genres tab", touchTab(TRENDING_AUDIO_TAB_TEXT));
        solo.waitForViewId(R.id.suggested_tracks_grid, 2000);
        assertEquals("Could not get to genres section", TRENDING_AUDIO_TAB_TEXT, currentTabTitle());
    }

    public void swipeRightToGenres() {
        solo.swipeRight();
        assertEquals("Could not get to genres section", GENRES_TAB_TEXT, currentTabTitle());
    }

    public void swipeLeftToTrendingAudio() {
        solo.swipeLeft();
        assertEquals("Could not get to trending audio section", TRENDING_AUDIO_TAB_TEXT, currentTabTitle());
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        PullToRefreshGridView view = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        ListAdapter adapter = view.getRefreshableView().getAdapter();
        int noOfItemsPlusPreloadingView = adapter.getCount() + 1;
        solo.scrollToBottom(view.getRefreshableView());
        assertTrue("New items in list did not load", waiter.waitForItemCountToIncrease(adapter, noOfItemsPlusPreloadingView));

    }


    private boolean touchTab(String tabText) {
        FixedWeightTabPageIndicator tabIndicator = (FixedWeightTabPageIndicator)solo.getView(R.id.indicator);
        List<View> touchableViews = tabIndicator.getChildAt(0).getTouchables();
        for(View view : touchableViews){
            if(((TextView)view).getText().equals(tabText)){
                solo.performClick(testCase, view);
                return true;
            }
        }
        return false;
    }

    public String currentTabTitle(){
        ViewPager viewPager = getViewPager();
        PagerAdapter pagerAdapter = viewPager.getAdapter();
        return pagerAdapter.getPageTitle(viewPager.getCurrentItem()).toString();
    }

    private ViewPager getViewPager() {
        return (ViewPager)solo.getView(R.id.pager);
    }

    public int getNumberOfItemsInGenresTab() {
        ListView currentListView = (ListView)solo.getView(R.id.suggested_tracks_categories_list);
        return currentListView.getAdapter().getCount();
    }

    public int getItemsOnTrendingMusicList(){
        return getItemsInSuggestedTracksGrid();
    }

    private int getItemsInSuggestedTracksGrid() {
        PullToRefreshGridView tracksList = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        return tracksList.getRefreshableView().getAdapter().getCount();
    }

    public int getItemsOnTrendingAudioList(){
        return getItemsInSuggestedTracksGrid();
    }

}
