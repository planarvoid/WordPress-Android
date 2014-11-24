package com.soundcloud.android.framework.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.Han;

public class AddToPlaylistsScreen extends Screen {

    public AddToPlaylistsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag("create_playlist_dialog");
    }
}
