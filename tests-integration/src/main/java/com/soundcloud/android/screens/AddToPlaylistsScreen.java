package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class AddToPlaylistsScreen extends Screen {

    private final Han testDriver;

    public AddToPlaylistsScreen(Han solo) {
        super(solo);
        testDriver = solo;
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public void waitForDialog() {
        testDriver.waitForFragmentByTag("create_playlist_dialog", Waiter.TIMEOUT);
    }
}
