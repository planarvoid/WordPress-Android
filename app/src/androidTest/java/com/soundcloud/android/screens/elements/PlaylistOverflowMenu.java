package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;

public class PlaylistOverflowMenu extends PopupMenuElement {

    private final static int SHUFFLE_ITEM_INDEX = 0;

    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public void shuffle() {
        shuffleItem().click();
    }

    private ViewElement shuffleItem() {
        return menuItems().get(SHUFFLE_ITEM_INDEX);
    }
}
