package com.soundcloud.android.screens.explore;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.viewpagerindicator.FixedWeightTabPageIndicator;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ExploreScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    private static final String GENRES_TAB_TEXT = "GENRES";
    private static final String TRENDING_AUDIO_TAB_TEXT = "TRENDING AUDIO";
    private static final String TRENDING_MUSIC_TAB_TEXT = "TRENDING MUSIC";
    private static final Predicate<TextView> TITLE_TEXT_VIEW_PREDICATE = new Predicate<TextView>() {
        @Override
        public boolean apply(TextView input) {
            return input.getId() == R.id.title;
        }
    };
    private MenuScreen menuScreen;

    public ExploreScreen(Han solo) {
        super(solo);

        waiter.waitForFragmentByTag("explore_fragment");

        waiter.waitForActivity(ACTIVITY);

        this.menuScreen = new MenuScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public void openExploreFromMenu(){
        menuScreen.openExplore();
    }

    public void touchGenresTab() {
        touchTab(GENRES_TAB_TEXT);
        waiter.waitForViewId(R.id.suggested_tracks_categories_list);
        waiter.waitForListContentAndRetryIfLoadingFailed();
        assertEquals("Could not get to genres section", GENRES_TAB_TEXT, currentTabTitle());
    }

    public void touchTrendingMusicTab() {
        touchTab(TRENDING_MUSIC_TAB_TEXT);
        solo.sleep(3000);
        waiter.waitForViewId(R.id.suggested_tracks_categories_list);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void clickElectronicGenre() {
        solo.clickOnText("Electronic");
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public ExploreGenreScreen clickGenreItem(String genreName) {
        solo.clickOnText(genreName);
        waiter.waitForListContentAndRetryIfLoadingFailed();
        return new ExploreGenreScreen(solo);
    }

    public void touchTrendingAudioTab() {
        assertTrue("Could not touch the genres tab", touchTab(TRENDING_AUDIO_TAB_TEXT));
        solo.waitForViewId(R.id.suggested_tracks_grid, 2000);
        assertEquals("Could not get to genres section", TRENDING_AUDIO_TAB_TEXT, currentTabTitle());
    }

    public void swipeRightToGenres() {
        solo.swipeRight();
        waiter.waitForListContentAndRetryIfLoadingFailed();
        //assertTrue("Swipe did not bring up expected tab", solo.waitForCondition(new CurrentTabTitleCondition(GENRES_TAB_TEXT), 2000));
    }

    public void swipeLeftToTrendingAudio() {
        solo.swipeLeft();
        assertTrue("Swipe did not bring up expected tab", solo.waitForCondition(new CurrentTabTitleCondition(TRENDING_AUDIO_TAB_TEXT), 2000));
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        PullToRefreshGridView view = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        ListAdapter adapter = view.getRefreshableView().getAdapter();
        int noOfItemsPlusPreloadingView = adapter.getCount() + 1;
        solo.scrollToBottom(view.getRefreshableView());
        assertTrue("New items in list did not load", waiter.waitForItemCountToIncrease(adapter, noOfItemsPlusPreloadingView));

    }

    public String playPopularTrack(int trackNumber) {
        PullToRefreshGridView gridView = (PullToRefreshGridView) solo.getView(R.id.suggested_tracks_grid);
        if (gridView == null) {
            throw new RuntimeException("No Tracks present");
        }
        View view = gridView.getRefreshableView().getChildAt(trackNumber);
        TextView textView = (TextView) view.findViewById(R.id.title);
        String title = textView.getText().toString();
        solo.clickOnView(view);
        return title;
    }

    private void validateThatClickedTrackMatchesExpectedTrackToPlay(List<TextView> textViewsForClickedItem, TrackSummary trackSummaryForPlayedTrack) {
        Optional<TextView> titleTextView = Iterables.tryFind(textViewsForClickedItem, TITLE_TEXT_VIEW_PREDICATE);
        if(!titleTextView.isPresent()){
            throw new IllegalStateException("Cannot find textview for explore track that has title");
        }
        assertEquals("Track title is not the same as the one that was clicked on", trackSummaryForPlayedTrack.getTitle(), titleTextView.get().getText());
    }


    private boolean touchTab(String tabText) {
        FixedWeightTabPageIndicator tabIndicator = (FixedWeightTabPageIndicator)solo.getView(R.id.indicator);
        List<View> touchableViews = tabIndicator.getChildAt(0).getTouchables();
        for(View view : touchableViews){
            if(((TextView)view).getText().equals(tabText)){
                solo.performClick(view);
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

    private class CurrentTabTitleCondition implements Condition {
        private String expectedTabString;

        private CurrentTabTitleCondition(String expectedTabString) {
            this.expectedTabString = expectedTabString;
        }

        @Override
        public boolean isSatisfied() {
            return currentTabTitle().equals(expectedTabString);
        }
    };

}
