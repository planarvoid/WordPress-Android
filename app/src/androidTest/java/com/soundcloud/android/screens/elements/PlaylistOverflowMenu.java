package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

public class PlaylistOverflowMenu extends PopupMenuElement {

    private static final int SHUFFLE_ITEM_INDEX = 0;
    private static final int MAKE_AVAILABLE_OFFLINE_INDEX = 1;
    ;

    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public void shuffle() {
        shuffleItem().click();
    }

    public PlaylistDetailsScreen clickMakeAvailableOffline() {
        menuItems().get(MAKE_AVAILABLE_OFFLINE_INDEX).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    private ViewElement shuffleItem() {
        return menuItems().get(SHUFFLE_ITEM_INDEX);
    }
}
