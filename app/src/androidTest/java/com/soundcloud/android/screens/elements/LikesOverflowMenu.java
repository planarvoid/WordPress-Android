package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.SyncYourLikesScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class LikesOverflowMenu extends PopupMenuElement {

    public LikesOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public SyncYourLikesScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new SyncYourLikesScreen(testDriver);
    }

    public UpgradeScreen clickUpsell() {
        getMakeAvailableOfflineItem().click();
        return new UpgradeScreen(testDriver);
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }
}
