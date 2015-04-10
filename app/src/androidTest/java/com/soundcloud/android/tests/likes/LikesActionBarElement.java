package com.soundcloud.android.tests.likes;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.SyncYourLikesScreen;
import com.soundcloud.android.screens.elements.ActionBarElement;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;


public class LikesActionBarElement extends ActionBarElement {

    public LikesActionBarElement(Han solo) {
        super(solo);
    }

    public ViewElement syncAction() {
        return testDriver.findElement(With.id(R.id.action_start_offline_update));
    }

    public SyncYourLikesScreen clickSyncLikesButton() {
        syncAction().click();
        return new SyncYourLikesScreen(testDriver);
    }

    public DownloadImageViewElement downloadElement() {
        return new DownloadImageViewElement(testDriver.findElement(With.id(R.id.header_download_state)));
    }
}
