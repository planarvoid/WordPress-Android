package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;

import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.support.v7.internal.view.menu.ListMenuItemView;

import java.util.List;

public class MenuElement {

    private final Han testDriver;

    public MenuElement(Han solo) {
        testDriver = solo;
    }

    public ViewElement addToPlaylistItem() {
        return menuItems().get(0);
    }

    public ViewElement shareItem() {
        return menuItems().get(1);
    }

    public ViewElement repostItem() {
        return menuItems().get(2);
    }

    private List<ViewElement> menuItems() {
        return testDriver.findElements(With.className(ListMenuItemView.class));
    }
}
