package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class AddToPlaylistScreen extends Screen {

    public AddToPlaylistScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("create_playlist_dialog");
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

    public VisualPlayerElement clickPlaylistWithTitleFromPlayer(String title) {
        pullToRefresh();
        playlists().scrollToItem(With.text(title)).click();
        return new VisualPlayerElement(testDriver);
    }

    private ListElement playlists() {
        return testDriver.findElement(With.id(R.id.select_dialog_listview)).toListView();
    }
}
