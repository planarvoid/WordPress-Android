package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;

public class PlaylistScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public PlaylistScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
