package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ConfirmDeletePlaylistScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
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

    public ViewElement getDeletePlaylistItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.delete_playlist)));
    }
}
