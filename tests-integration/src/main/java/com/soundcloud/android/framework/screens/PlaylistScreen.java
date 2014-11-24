package com.soundcloud.android.framework.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.screens.elements.ListElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class PlaylistScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public PlaylistScreen(Han solo) {
        super(solo);
    }

    public void clickPlaylistAt(int index) {
        playlistsList().getItemAt(index).click();
    }

    public PlaylistDetailsScreen clickPlaylist(With matcher) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.scrollToBottom(testDriver.getCurrentListView());
        testDriver.findElement(matcher).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private ListElement playlistsList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }
}
