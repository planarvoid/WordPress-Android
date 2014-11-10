package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.tests.with.With;

import android.widget.TextView;

import java.util.List;

public class TrackItemMenuElement {
    private final Han testDriver;
    private final Waiter waiter;

    public TrackItemMenuElement(Han solo) {
        testDriver = solo;
        waiter = new Waiter(testDriver);
    }

    public ViewElement likeItem() {
        return menuItems().get(0);
    }

    public ViewElement addToPlaylistItem() {
        return menuItems().get(1);
    }

    public String getLikeItemTitle() {
        return menuItems().get(0).getText().toString();
    }

    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private List<ViewElement> menuItems() {
        waiter.waitForElement(TextView.class);
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }
}
