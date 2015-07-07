package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.SyncYourLikesScreen;

public class LikesOverflowMenu extends PopupMenuElement {

    public LikesOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public SyncYourLikesScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new SyncYourLikesScreen(testDriver);
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }
}
