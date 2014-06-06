package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.view.SlidingTabLayout;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class ExploreScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    private static final String GENRES_TAB_TEXT = "GENRES";
    private static final String TRENDING_AUDIO_TAB_TEXT = "AUDIO";
    private static final String TRENDING_MUSIC_TAB_TEXT = "MUSIC";
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

    private SlidingTabs tabs() {
        return testDriver.findElement(R.id.sliding_tabs).toSlidingTabs();
    }

    public void touchGenresTab() {
        touchTab(GENRES_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void touchTrendingMusicTab() {
        touchTab(TRENDING_MUSIC_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public ExploreGenreCategoryScreen clickElectronicGenre() {
        testDriver.clickOnText("Electronic");
        return new ExploreGenreCategoryScreen(testDriver);
    }

    public ExploreGenreCategoryScreen clickGenreItem(String genreName) {
        testDriver.clickOnText(genreName, true);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ExploreGenreCategoryScreen(testDriver);
    }

    public void touchTrendingAudioTab() {
        touchTab(TRENDING_AUDIO_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public String getTrackTitle(int index) {
        View view = ((GridView) viewPager.getCurrentPage(GridView.class)).getChildAt(index);

        TextView textView = (TextView) view.findViewById(R.id.title);
        return textView.getText().toString();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        testDriver.scrollToBottom((GridView) viewPager.getCurrentPage(GridView.class));
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public LegacyPlayerScreen playPopularTrack(int trackNumber) {
        View view = ((GridView) viewPager.getCurrentPage(GridView.class)).getChildAt(trackNumber);
        testDriver.clickOnView(view);
        return new LegacyPlayerScreen(testDriver);
    }

    public void playFirstTrack() {
        View view = ((GridView) viewPager.getCurrentPage(GridView.class)).getChildAt(0);
        testDriver.clickOnView(view);
    }

    private void validateThatClickedTrackMatchesExpectedTrackToPlay(List<TextView> textViewsForClickedItem, TrackSummary trackSummaryForPlayedTrack) {
        Optional<TextView> titleTextView = Iterables.tryFind(textViewsForClickedItem, TITLE_TEXT_VIEW_PREDICATE);
        if(!titleTextView.isPresent()){
            throw new IllegalStateException("Cannot find textview for explore track that has title");
        }
        assertEquals("Track title is not the same as the one that was clicked on", trackSummaryForPlayedTrack.getTitle(), titleTextView.get().getText());
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    public String currentTabTitle(){
        List<View> indicatorItems = getViewPagerIndicator().getChildAt(0).getTouchables();
        TextView selectedItem = (TextView) indicatorItems.get(getViewPager().getCurrentItem());
        return selectedItem.getText().toString();
    }

    private ViewPager getViewPager() {
        return (ViewPager) testDriver.getView(R.id.pager);
    }

    private SlidingTabLayout getViewPagerIndicator() {
        return (SlidingTabLayout) testDriver.getView(R.id.sliding_tabs);
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
            return (ListView) viewPager.getCurrentPage(ListView.class);
        }
        //TODO: Don't return nulls
        return null;
    }

    private GridView musicPage() {
        if (currentTabTitle().equals(TRENDING_MUSIC_TAB_TEXT)) {
            return (GridView) viewPager.getCurrentPage(GridView.class);
        }
        //TODO: Don't return nulls
        return null;
    }

    private GridView audioPage() {
        if (currentTabTitle().equals(TRENDING_AUDIO_TAB_TEXT)) {
            return (GridView) viewPager.getCurrentPage(GridView.class);
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
