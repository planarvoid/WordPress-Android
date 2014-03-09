package com.soundcloud.android.screens;

import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

import android.widget.GridView;

public class PlaylistResultsScreen extends Screen {

    private static final Class ACTIVITY = CombinedSearchActivity.class;

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
        solo.getSolo().clickInList(position);
        return new PlaylistDetailsScreen(solo);
    }

    public PlaylistTagsScreen pressBack() {
        solo.goBack();
        return new PlaylistTagsScreen(solo);
    }

    private GridView getList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return solo.getCurrentGridView();
    }

}
