package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import java.util.List;

public class TrackItemMenuElement {
    private final Han testDriver;

    public TrackItemMenuElement(Han solo) {
        testDriver = solo;
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
