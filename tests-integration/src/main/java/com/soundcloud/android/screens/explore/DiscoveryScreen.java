package com.soundcloud.android.screens.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.ListView;
import android.widget.TextView;

public class DiscoveryScreen extends Screen {

    private Han solo;
    private Waiter waiter;

    public DiscoveryScreen(Han solo) {
        super(solo);
        this.solo = solo;
        this.waiter = new Waiter(solo);
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
//        FixedWeightTabPageIndicator tabIndicator = (FixedWeightTabPageIndicator)solo.getView(R.id.indicator);
        solo.clickOnText("Genres");
        solo.waitForViewId(R.id.suggested_tracks_categories_list,5000);
    }

    public String currentTabTitle(){
        ViewPager viewPager = getViewPager();
        PagerAdapter pagerAdapter = viewPager.getAdapter();
        return pagerAdapter.getPageTitle(viewPager.getCurrentItem()).toString();
    }

    private ViewPager getViewPager() {
        return (ViewPager)solo.getView(R.id.pager);
    }

    public void clickPopularAudioTab() {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void clickGenre(int genre) {
        solo.clickOnText(genre);
    }

    public int getNumberOfItemsInGenresTab() {
        ListView currentListView = (ListView)solo.getView(R.id.suggested_tracks_categories_list);
        return currentListView.getAdapter().getCount();
    }

}
