package com.soundcloud.android.screens.elements;

import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.with.With;

import android.widget.TextView;

import java.util.List;

public class PlayerMenuElement {

    private final Han testDriver;
    private final Waiter waiter;

    public PlayerMenuElement(Han solo) {
        testDriver = solo;
        waiter = new Waiter(testDriver);
        waiter.waitForElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    public TrackInfoScreen clickInfo() {
        info().click();
        final TrackInfoScreen trackInfoScreen = new TrackInfoScreen(testDriver);
        trackInfoScreen.waitForDialog();
        return trackInfoScreen;
    }

    public AddToPlaylistsScreen clickAddToPlaylist() {
        addToPlaylistItem().click();
        return new AddToPlaylistsScreen(testDriver);
    }

    public AddCommentScreen clickComment() {
        comment().click();
        return new AddCommentScreen(testDriver);
    }

    private ViewElement addToPlaylistItem() {
        return menuItems().get(0);
    }

    public ViewElement shareItem() {
        return menuItems().get(1);
    }

    public ViewElement repostItem() {
        return menuItems().get(2);
    }

    private ViewElement comment() {
        return menuItems().get(3);
    }

    private ViewElement info() {
        return menuItems().get(4);
    }


    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private List<ViewElement> menuItems() {
        waiter.waitForElement(TextView.class);
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }
}
