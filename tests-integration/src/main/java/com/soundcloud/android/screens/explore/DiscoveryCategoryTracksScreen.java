package com.soundcloud.android.screens.explore;

import static junit.framework.Assert.assertTrue;

import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.widget.ListAdapter;

public class DiscoveryCategoryTracksScreen extends Screen {

    private Waiter waiter;
    public DiscoveryCategoryTracksScreen(Han solo) {
        super(solo);
        waiter = new Waiter(solo);
    }

    public int getItemsOnList() {
        PullToRefreshGridView tracksList = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        return tracksList.getRefreshableView().getAdapter().getCount();
    }


    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        PullToRefreshGridView view = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        ListAdapter adapter = view.getRefreshableView().getAdapter();
        solo.scrollToBottom(view.getRefreshableView());
        int currentSize = adapter.getCount();
        waiter.waitForItemCountToIncrease(adapter, currentSize);
        assertTrue("New items in list did not load", currentSize < adapter.getCount());
    }
}
