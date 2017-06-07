package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.playlists.DeletePlaylistDialogFragment;
import com.soundcloud.android.screens.PlaylistsScreen;

public class ConfirmDeletePlaylistDialogElement extends Element {

    ConfirmDeletePlaylistDialogElement(Han testDriver) {
        super(testDriver, With.text(R.string.dialog_playlist_delete_title));
        waiter.assertForFragmentByTag(DeletePlaylistDialogFragment.TAG);
    }

    public void clickCancel() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.btn_cancel))).click();
    }

    public PlaylistsScreen clickConfirm() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist))).click();
        return new PlaylistsScreen(testDriver);
    }
}
