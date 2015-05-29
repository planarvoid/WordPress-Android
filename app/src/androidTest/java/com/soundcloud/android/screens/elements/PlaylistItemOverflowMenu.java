package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistsScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public PlaylistsScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new PlaylistsScreen(testDriver);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals(testDriver.getString(R.string.unlike));
    }

    private ViewElement likeItem() {
        return menuItem(With.text(testDriver.getString(R.string.like), testDriver.getString(R.string.unlike)));
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return menuItem(With.text(testDriver.getString(R.string.make_offline_available)));
    }
}
