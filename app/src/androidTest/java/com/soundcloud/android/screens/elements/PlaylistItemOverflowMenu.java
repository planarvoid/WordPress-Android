package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.PlaylistsScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    private static final int ITEMS_IN_MENU_WITH_OFFLINE_SYNC = 2;
    private static final int MAKE_AVAILABLE_OFFLINE_INDEX = 0;
    private static final int TOGGLE_LIKE_INDEX = 1;

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public PlaylistsScreen clickMakeAvailableOffline() {
        menuItems().get(MAKE_AVAILABLE_OFFLINE_INDEX).click();
        return new PlaylistsScreen(testDriver);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals("Unlike");
    }

    private ViewElement likeItem() {
        if (menuItems().size() == ITEMS_IN_MENU_WITH_OFFLINE_SYNC) {
            return menuItems().get(TOGGLE_LIKE_INDEX);
        }
        return menuItems().get(TOGGLE_LIKE_INDEX - 1);
    }
}
