package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SearchResultsScreen extends Screen {
    private static final Class ACTIVITY = CombinedSearchActivity.class;

    public SearchResultsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    public PlayerScreen clickFirstTrackItem() {
        View itemView = getFirstItemOfClass(Track.class);
        solo.clickOnView(itemView);
        return new PlayerScreen(solo);
    }

    public PlaylistScreen clickFirstPlaylistItem() {
        View itemView = getFirstItemOfClass(Playlist.class);
        solo.clickOnView(itemView);
        return new PlaylistScreen(solo);
    }

    public ProfileScreen clickFirstUserItem() {
        View itemView = getFirstItemOfClass(User.class);
        solo.clickOnView(itemView);
        return new ProfileScreen(solo);
    }

    public MainScreen pressBack() {
        solo.goBack();
        // A supposition is made that the previous screen was the main screen
        return new MainScreen(solo);
    }

    public int getResultItemCount() {
        return resultsList().getAdapter().getCount();
    }

    private ListView resultsList() {
        return solo.getCurrentListView();
    }

    private View getFirstItemOfClass(Class itemClass) {
        ListAdapter adapter = solo.getCurrentListView().getAdapter();
        int numberOfItems = adapter.getCount();
        for (int i = 0; i < numberOfItems; i++) {
            if(itemClass.isInstance(adapter.getItem(i))) {
                return solo.getCurrentListView().getChildAt(i);
            }
        }
        return null;
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForElement(R.id.searchResultsContainer);
    }
}
