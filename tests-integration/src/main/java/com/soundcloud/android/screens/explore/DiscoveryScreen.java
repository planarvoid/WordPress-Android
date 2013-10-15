package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;
import com.viewpagerindicator.FixedWeightTabPageIndicator;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class DiscoveryScreen extends Screen {

    private Han solo;
    private Waiter waiter;
    private InstrumentationTestCase testCase;

    public DiscoveryScreen(Han solo, Waiter waiter, InstrumentationTestCase testCase) {
        super(solo);
        this.solo = solo;
        this.waiter = waiter;
        this.testCase = testCase;
    }

    public String getActiveTabName() {
        return ((TextView)solo.getView(R.id.nav_explore)).getText().toString();
    }

    public int getItemsOnList() {
        return 0;
    }

    public void scrollDown() {

    }

    public String clickTrack(int i) {
        return "Track name";
    }

    public void touchGenresTab() {
        assertTrue("Could not touch the genres tab", touchTab("GENRES"));
        solo.waitForViewId(R.id.suggested_tracks_categories_list, 5000);
    }

    public void clickElectronicGenre() {
        solo.clickOnText("Electronic");
        waiter.waitForListContent();
    }

    public void touchTrendingAudioTab() {
        assertTrue("Could not touch the genres tab", touchTab("TRENDING AUDIO"));
        solo.waitForViewId(R.id.suggested_tracks_grid, 5000);
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

}
