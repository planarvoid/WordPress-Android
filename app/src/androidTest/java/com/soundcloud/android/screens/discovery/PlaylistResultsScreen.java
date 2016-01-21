package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.Screen;

import android.support.v7.widget.RecyclerView;

public class PlaylistResultsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDiscoveryActivity.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public PlaylistResultsScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("playlist_results");
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag("playlist_results");
    }

    public int getResultsCount() {
        return resultList().getItemCount();
    }

    public PlaylistDetailsScreen clickOnFirstPlaylist() {
        testDriver.findOnScreenElement(With.id(R.id.playlist_list_item)).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public DiscoveryScreen pressBack() {
        testDriver.goBack();
        return new DiscoveryScreen(testDriver);
    }

    private RecyclerViewElement resultList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

}
