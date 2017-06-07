package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class PlaylistOverflowMenu extends PopupMenuElement {
    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public VisualPlayerElement shuffle() {
        shuffleItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public ConfirmDeletePlaylistDialogElement clickDelete() {
        findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist))).click();
        return new ConfirmDeletePlaylistDialogElement(testDriver);
    }

    private ViewElement shuffleItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.shuffle_playlist)));
    }
}
