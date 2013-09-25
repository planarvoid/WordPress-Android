package com.soundcloud.android.screens.explore;

import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

public class DiscoveryCategoryTracksScreen extends Screen {

    public DiscoveryCategoryTracksScreen(Han solo) {
        super(solo);
    }

    public int getItemsOnList() {
        PullToRefreshGridView tracksList = (PullToRefreshGridView)solo.getView(R.id.suggested_tracks_grid);
        return tracksList.getRefreshableView().getAdapter().getCount();
    }

}
