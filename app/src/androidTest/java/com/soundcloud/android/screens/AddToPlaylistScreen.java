package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class AddToPlaylistScreen extends Screen {

    public static final String FRAGMENT_TAG = "create_playlist_dialog";

    public AddToPlaylistScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT_TAG);
    }

    public CreatePlaylistScreen clickCreateNewPlaylist() {
        testDriver.findOnScreenElements(With.text(testDriver.getString(R.string.create_new_playlist))).get(0).click();
        return new CreatePlaylistScreen(testDriver);
    }

    public VisualPlayerElement clickPlaylistWithTitleFromPlayer(String title) {
        pullToRefresh();
        scrollToItem(With.text(title)).click();
        return new VisualPlayerElement(testDriver);
    }

}
