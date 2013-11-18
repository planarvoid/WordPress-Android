package com.soundcloud.android.screens.explore;

import android.widget.ListAdapter;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.explore.ExploreTracksCategoryActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

import static junit.framework.Assert.assertTrue;

public class ExploreGenreScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public ExploreGenreScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ExploreTracksCategoryActivity.class);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
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
