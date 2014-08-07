package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.tests.with.With;

import android.support.v7.internal.view.menu.ListMenuItemView;

import java.util.List;

public class MenuElement {

    private final Han testDriver;
    private final Waiter waiter;

    public MenuElement(Han solo) {
        testDriver = solo;
        waiter = new Waiter(testDriver);
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

    public ViewElement info() {
        return menuItems().get(3);
    }

    private ViewElement container() {
        return testDriver.findElement(With.classStringName("android.widget.PopupWindow$PopupViewContainer"));
    }

    private List<ViewElement> menuItems() {
        waiter.waitForElement(ListMenuItemView.class);
        return container().findElements(With.className(ListMenuItemView.class));
    }
}
