package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class ConfirmDisableSyncCollectionScreen extends Screen {

    private final Class activity;

    public ConfirmDisableSyncCollectionScreen(Han solo, Class activity) {
        super(solo);
        this.activity = activity;
    }

    @Override
    protected Class getActivity() {
        return activity;
    }

    public void clickCancel() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(android.R.string.cancel))).click();
        waiter.waitForDialogToClose();
    }

    public void clickOk() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(android.R.string.ok))).click();
        waiter.waitForDialogToClose();
    }

    public PlaylistDetailsScreen clickCancelForPlaylistDetails() {
        clickCancel();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistDetailsScreen clickOkForPlaylistDetails() {
        clickOk();
        return new PlaylistDetailsScreen(testDriver);
    }

}