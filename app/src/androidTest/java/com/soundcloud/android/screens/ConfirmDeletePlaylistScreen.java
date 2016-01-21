package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class ConfirmDeletePlaylistScreen extends Screen {

    private final Class activity;

    public ConfirmDeletePlaylistScreen(Han solo, Class activity) {
        super(solo);
        this.activity = activity;
    }

    @Override
    protected Class getActivity() {
        return activity;
    }

    public void clickCancel() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.btn_cancel))).click();
    }

    public void clickConfirm() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist))).click();
    }
}
