package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.ConfirmDeletePlaylistScreen;

public class PlaylistOverflowMenu extends PopupMenuElement {
    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public VisualPlayerElement shuffle() {
        shuffleItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public ConfirmDeletePlaylistScreen clickDelete() {
        findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist))).click();
        return new ConfirmDeletePlaylistScreen(testDriver, PlaylistDetailActivity.class);
    }

    private ViewElement shuffleItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.shuffle_playlist)));
    }
}
