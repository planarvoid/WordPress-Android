package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public CollectionsScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new CollectionsScreen(testDriver);
    }

    public UpgradeScreen clickUpsell() {
        getMakeAvailableOfflineItem().click();
        return new UpgradeScreen(testDriver);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public void toggleRepost() {
        repostItem().click();
    }

    public boolean isReposted() {
        return getElementText(repostItem()).equals(testDriver.getString(R.string.unpost));
    }

    private ViewElement likeItem() {
        return findElement(With.text(testDriver.getString(R.string.btn_like), testDriver.getString(R.string.btn_unlike)));
    }

    private ViewElement repostItem() {
        return findElement(With.text(testDriver.getString(R.string.repost), testDriver.getString(R.string.unpost)));
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals(testDriver.getString(R.string.btn_unlike));
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }
}
