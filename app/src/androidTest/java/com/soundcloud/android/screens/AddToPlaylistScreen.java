package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;

public class AddToPlaylistScreen extends Screen {

    public AddToPlaylistScreen(Han solo) {
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

    public CreatePlaylistScreen clickCreateNewPlaylist() {
        testDriver.findElements(With.text(testDriver.getString(R.string.create_new_playlist))).get(0).click();

        return new CreatePlaylistScreen(testDriver);
    }
}
