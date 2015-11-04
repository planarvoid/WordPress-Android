package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.search.LegacySearchActivity;

import android.support.v7.widget.RecyclerView;

public class PlaylistResultsScreen extends Screen {

    private static final Class ACTIVITY = LegacySearchActivity.class;

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
        return resultList().getItemCount();
    }

    public PlaylistDetailsScreen clickOnPlaylist(int position) {
        resultList().getItemAt(position).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistTagsScreen pressBack() {
        testDriver.goBack();
        return new PlaylistTagsScreen(testDriver);
    }

    private RecyclerViewElement resultList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElement(With.className(RecyclerView.class)).toRecyclerView();
    }

}
