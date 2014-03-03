package com.soundcloud.android.screens;

import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

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
        return solo.getCurrentListView().getAdapter().getCount();
    }

    public PlaylistDetailsScreen clickOnPlaylist(int position) {
        solo.getSolo().clickInList(position);
        return new PlaylistDetailsScreen(solo);
    }

}
