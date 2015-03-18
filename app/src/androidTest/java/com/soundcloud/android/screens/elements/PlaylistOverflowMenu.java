package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.List;

public class PlaylistOverflowMenu {

    private static int SHUFFLE_ITEM_INDEX = 0;

    private final Han testDriver;

    public PlaylistOverflowMenu(Han testDriver) {
        this.testDriver = testDriver;
    }

    public void shuffle() {
        shuffleItem().click();
    }

    private List<ViewElement> menuItems() {
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private ViewElement shuffleItem() {
        return menuItems().get(SHUFFLE_ITEM_INDEX);
    }
}
