package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.List;

public class TrackItemMenuElement {
    private final Han testDriver;

    public TrackItemMenuElement(Han solo) {
        testDriver = solo;
    }

    public void toggleLike() {
        menuItems().get(0).click();
    }

    public void clickAdToPlaylist() {
        menuItems().get(1).click();
    }

    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private List<ViewElement> menuItems() {
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }
}
