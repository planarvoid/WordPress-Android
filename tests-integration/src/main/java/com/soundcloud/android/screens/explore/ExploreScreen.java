package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.tests.Han;
import com.viewpagerindicator.FixedWeightTabPageIndicator;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

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

    private ViewPagerElement viewPager;

    public ExploreScreen(Han solo) {
        super(solo);

        waiter.waitForFragmentByTag("explore_fragment");

        waiter.waitForActivity(ACTIVITY);
        viewPager = new ViewPagerElement(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public void touchGenresTab() {
        touchTab(GENRES_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public void touchTrendingMusicTab() {
        touchTab(TRENDING_MUSIC_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public ExploreGenreCategoryScreen clickElectronicGenre() {
        solo.clickOnText("Electronic");
        return new ExploreGenreCategoryScreen(solo);
    }

    public ExploreGenreCategoryScreen clickGenreItem(String genreName) {
        solo.clickOnText(genreName);
        waiter.waitForListContentAndRetryIfLoadingFailed();
        return new ExploreGenreCategoryScreen(solo);
    }

    public void touchTrendingAudioTab() {
        touchTab(TRENDING_AUDIO_TAB_TEXT);
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public String getTrackTitle(int index) {
        View view = ((GridView)viewPager.getCurrentPage(GridView.class)).getChildAt(index);

        TextView textView = (TextView) view.findViewById(R.id.title);
        return textView.getText().toString();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        solo.scrollToBottom((GridView) viewPager.getCurrentPage(GridView.class));
        waiter.waitForListContentAndRetryIfLoadingFailed();
    }

    public PlayerScreen playPopularTrack(int trackNumber) {
        View view = ((GridView)viewPager.getCurrentPage(GridView.class)).getChildAt(trackNumber);
        solo.clickOnView(view);
        return new PlayerScreen(solo);
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
        return genresPage().getAdapter().getCount();
    }

    public int getItemsOnGenresList() {
        return genresPage().getAdapter().getCount();
    }

    public int getItemsOnTrendingMusicList(){
        return musicPage().getAdapter().getCount();
    }

    public int getItemsOnTrendingAudioList(){
        return audioPage().getAdapter().getCount();
    }

    private ListView genresPage() {
        if (currentTabTitle().equals(GENRES_TAB_TEXT)) {
            return (ListView)viewPager.getCurrentPage(ListView.class);
        }
        //TODO: Don't return nulls
        return null;
    }

    private GridView musicPage() {
        if (currentTabTitle().equals(TRENDING_MUSIC_TAB_TEXT)) {
            return (GridView)viewPager.getCurrentPage(GridView.class);
        }
        //TODO: Don't return nulls
        return null;
    }

    private GridView audioPage() {
        if (currentTabTitle().equals(TRENDING_AUDIO_TAB_TEXT)) {
            return (GridView)viewPager.getCurrentPage(GridView.class);
        }
        //TODO: Don't return nulls
        return null;
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
