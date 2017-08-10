package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.settings.OfflineSettingsActivity;

public class ConfirmRemoveOfflineContentScreen extends Screen {

    public ConfirmRemoveOfflineContentScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return OfflineSettingsActivity.class;
    }

    public void clickConfirm() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.btn_continue))).click();
        waiter.waitForDialogToClose();
    }

}
