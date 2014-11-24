package com.soundcloud.android.framework.screens.elements;

import com.soundcloud.android.framework.Han;

import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;

import com.soundcloud.android.framework.with.With;

import java.util.List;

public class TrackItemMenuElement {
    private final Han testDriver;

    public TrackItemMenuElement(Han solo) {
        testDriver = solo;
    }


    public void clickAdToPlaylist() {
        menuItems().get(1).click();
    }

    public String getLikeItemTitle() {
        return new TextElement(menuItems().get(0)).getText().toString();
    }

    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private List<ViewElement> menuItems() {
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }
}
