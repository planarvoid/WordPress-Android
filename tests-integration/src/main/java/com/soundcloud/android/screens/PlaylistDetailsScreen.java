package com.soundcloud.android.screens;

import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.tests.Han;

public class PlaylistDetailsScreen extends Screen {
    private static final Class ACTIVITY = PlaylistDetailActivity.class;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
