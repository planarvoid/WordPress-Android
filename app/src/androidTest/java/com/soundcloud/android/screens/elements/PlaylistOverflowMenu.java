package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.ConfirmDeletePlaylistScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class PlaylistOverflowMenu extends PopupMenuElement {
    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public VisualPlayerElement shuffle() {
        shuffleItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public PlaylistDetailsScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistDetailsScreen clickMakeUnavailableOffline() {
        getMakeUnvailableOfflineItem().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public UpgradeScreen clickUpsell() {
        getMakeAvailableOfflineItem().click();
        return new UpgradeScreen(testDriver);
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }

    public ViewElement getMakeUnvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_unavailable)));
    }

    public ConfirmDeletePlaylistScreen clickDelete() {
        findElement(With.text(testDriver.getString(R.string.delete_playlist))).click();
        return new ConfirmDeletePlaylistScreen(testDriver, PlaylistDetailActivity.class);
    }

    private ViewElement shuffleItem() {
        return findElement(With.text(testDriver.getString(R.string.shuffle_playlist)));
    }
}
