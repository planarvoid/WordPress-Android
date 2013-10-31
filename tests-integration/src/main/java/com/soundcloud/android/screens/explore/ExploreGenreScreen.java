package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertTrue;

import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Waiter;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListAdapter;

public class ExploreGenreScreen extends Screen {

    private Waiter waiter;

    public ExploreGenreScreen(ActivityInstrumentationTestCase2 testCase) {
        super(testCase);
        waiter = new Waiter(solo);
    }

    public int getItemsOnList() {
        PullToRefreshGridView tracksList = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        return tracksList.getRefreshableView().getAdapter().getCount();
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        PullToRefreshGridView view = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        ListAdapter adapter = view.getRefreshableView().getAdapter();
        int noOfItemsPlusPreloadingView = adapter.getCount() + 1;
        solo.scrollToBottom(view.getRefreshableView());
        assertTrue("New items in list did not load", waiter.waitForItemCountToIncrease(adapter, noOfItemsPlusPreloadingView));
    }

}
