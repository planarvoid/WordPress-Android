package com.soundcloud.android.screens.explore;

import static com.google.common.base.Preconditions.checkArgument;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.PlayerActivity;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Waiter;
import com.viewpagerindicator.FixedWeightTabPageIndicator;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class ExploreScreen extends Screen {

    private static final String GENRES_TAB_TEXT = "GENRES";
    private static final String TRENDING_AUDIO_TAB_TEXT = "TRENDING AUDIO";
    private static final String TRENDING_MUSIC_TAB_TEXT = "TRENDING MUSIC";
    private static final Predicate<TextView> TITLE_TEXT_VIEW_PREDICATE = new Predicate<TextView>() {
        @Override
        public boolean apply(TextView input) {
            return input.getId() == R.id.title;
        }
    };
    private Waiter waiter;
    private MenuScreen menuScreen;

    public ExploreScreen(ActivityInstrumentationTestCase2 testCase) {
        super(testCase);
        this.waiter = new Waiter(solo);
        this.menuScreen = new MenuScreen(solo);
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
        assertTrue("Swipe did not bring up expected tab", solo.waitForCondition(new CurrentTabTitleCondition(GENRES_TAB_TEXT), 2000));
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
        PullToRefreshGridView tracksList = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        ListAdapter exploreTracksAdapter = tracksList.getRefreshableView().getAdapter();
        checkArgument(trackNumber <= exploreTracksAdapter.getCount(), "Not enough items in list to play track " + trackNumber);
        solo.scrollToItem(trackNumber-1);
        List<TextView> textViewsForClickedItem = solo.clickInList(1);
        solo.waitForActivity(PlayerActivity.class);
        TrackSummary trackSummaryForPlayedTrack = (TrackSummary)exploreTracksAdapter.getItem(trackNumber - 1);
        validateThatClickedTrackMatchesExpectedTrackToPlay(textViewsForClickedItem, trackSummaryForPlayedTrack);
        return trackSummaryForPlayedTrack.getTitle();
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
