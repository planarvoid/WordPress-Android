package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public PlaylistsScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new PlaylistsScreen(testDriver);
    }

    public UpgradeScreen clickUpsell() {
        getMakeAvailableOfflineItem().click();
        return new UpgradeScreen(testDriver);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals(testDriver.getString(R.string.unlike));
    }

    private ViewElement likeItem() {
        return findElement(With.text(testDriver.getString(R.string.like), testDriver.getString(R.string.unlike)));
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }
}
