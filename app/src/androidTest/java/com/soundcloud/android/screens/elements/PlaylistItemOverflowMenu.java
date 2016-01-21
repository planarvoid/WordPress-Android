package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.ConfirmDeletePlaylistScreen;
import com.soundcloud.android.screens.ConfirmDisableSyncCollectionScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public CollectionScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new CollectionScreen(testDriver);
    }

    public CollectionScreen clickMakeUnavailableOffline() {
        getMakeUnavailableOfflineItem().click();
        return new CollectionScreen(testDriver);
    }

    public ConfirmDisableSyncCollectionScreen clickMakeUnavailableOfflineToDisableSyncCollection() {
        getMakeUnavailableOfflineItem().click();
        return new ConfirmDisableSyncCollectionScreen(testDriver, MainActivity.class);
    }

    public UpgradeScreen clickUpsell() {
        getMakeAvailableOfflineItem().click();
        return new UpgradeScreen(testDriver);
    }

    public ConfirmDeletePlaylistScreen clickDelete() {
        getDeletePlaylistItem().click();
        return new ConfirmDeletePlaylistScreen(testDriver, MainActivity.class);
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
        return findOnScreenElement(With.text(testDriver.getString(R.string.btn_like), testDriver.getString(R.string.btn_unlike)));
    }

    private ViewElement repostItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.repost), testDriver.getString(R.string.unpost)));
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals(testDriver.getString(R.string.btn_unlike));
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }

    public ViewElement getMakeUnavailableOfflineItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.make_offline_unavailable)));
    }

    public ViewElement getDeletePlaylistItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist)));
    }
}
