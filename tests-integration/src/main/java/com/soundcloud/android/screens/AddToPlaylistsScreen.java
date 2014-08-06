package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;

public class AddToPlaylistsScreen extends Screen {

    public AddToPlaylistsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public void waitForDialog() {
        waiter.waitForFragmentByTag("create_playlist_dialog");
    }
}
