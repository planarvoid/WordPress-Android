package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.SyncYourLikesScreen;

public class LikesOverflowMenu extends PopupMenuElement {

    private static final int MAKE_AVAILABLE_OFFLINE_INDEX = 0;

    public LikesOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public SyncYourLikesScreen clickMakeAvailableOffline() {
        makeAvailableOfflineItem().click();
        return new SyncYourLikesScreen(testDriver);
    }

    private ViewElement makeAvailableOfflineItem() {
        return menuItems().get(MAKE_AVAILABLE_OFFLINE_INDEX);
    }


}
