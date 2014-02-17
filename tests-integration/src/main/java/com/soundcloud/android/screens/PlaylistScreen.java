package com.soundcloud.android.screens;

import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.tests.Han;

public class PlaylistScreen extends Screen {
    private static final Class ACTIVITY = PlaylistDetailActivity.class;

    public PlaylistScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
