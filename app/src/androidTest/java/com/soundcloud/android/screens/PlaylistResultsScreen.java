package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.search.SearchActivity;

import android.widget.GridView;

public class PlaylistResultsScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public PlaylistResultsScreen(Han solo) {
        super(solo);
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag("playlist_results");
    }

    public int getResultsCount() {
        return getList().getAdapter().getCount();
    }

    public PlaylistDetailsScreen clickOnPlaylist(int position) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.getSolo().clickInList(position);
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistTagsScreen pressBack() {
        testDriver.goBack();
        return new PlaylistTagsScreen(testDriver);
    }

    private GridView getList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.getCurrentGridView();
    }

}
