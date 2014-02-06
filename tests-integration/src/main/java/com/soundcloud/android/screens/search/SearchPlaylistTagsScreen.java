package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

public class SearchPlaylistTagsScreen extends Screen {
    private static final Class ACTIVITY = CombinedSearchActivity.class;

    public SearchPlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForElement(R.id.playlistTagsContainer);
    }
}
