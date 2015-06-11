package com.soundcloud.android.screens.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

public class ExploreScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    private static final String GENRES_TAB_TEXT = "GENRES";
    private static final String TRENDING_AUDIO_TAB_TEXT = "AUDIO";
    private static final String TRENDING_MUSIC_TAB_TEXT = "MUSIC";

    private ViewPagerElement viewPager;

    public ExploreScreen(Han solo) {
        super(solo);
        viewPager = new ViewPagerElement(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private SlidingTabs tabs() {
        return testDriver.findElement(With.id(R.id.sliding_tabs)).toSlidingTabs();
    }

    public void touchGenresTab() {
        touchTab(GENRES_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void touchTrendingMusicTab() {
        touchTab(TRENDING_MUSIC_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public ExploreGenreCategoryScreen clickGenreItem(String genreName) {
        testDriver.clickOnText(genreName);
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

    public VisualPlayerElement playPopularTrack(int trackNumber) {
        View view = ((GridView) viewPager.getCurrentPage(GridView.class)).getChildAt(trackNumber);
        testDriver.wrap(view).click();
        return new VisualPlayerElement(testDriver);
    }

    public void playFirstTrack() {
        VisualPlayerElement player = new VisualPlayerElement(testDriver);
        ViewElement gridView = testDriver.findElement(With.className(GridView.class));
        gridView.getChildAt(0).click();
        player.waitForExpandedPlayer();
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    public String currentTabTitle() {
        int currentPage = getViewPager().getCurrentItem();
        return new TextElement(tabs().getTabAt(currentPage)).getText();
    }

    private ViewPager getViewPager() {
        return testDriver.findElement(With.id(R.id.pager)).toViewPager();
    }

    public int getItemsOnTrendingMusicList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return musicPage().getAdapter().getCount();
    }

    public int getItemsOnTrendingAudioList(){
        waiter.waitForContentAndRetryIfLoadingFailed();
        return audioPage().getAdapter().getCount();
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
}
