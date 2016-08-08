package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.screens.stations.StationHomeScreen;

public class PlayerMenuElement extends PopupMenuElement {

    public PlayerMenuElement(Han solo) {
        super(solo);
    }

    public TrackInfoScreen clickInfo() {
        clickMenuElementForFragment(info(), TrackInfoScreen.FRAGMENT_TAG);
        return new TrackInfoScreen(testDriver);
    }

    public AddToPlaylistScreen clickAddToPlaylist() {
        clickMenuElementForFragment(addToPlaylistItem(), AddToPlaylistScreen.FRAGMENT_TAG);
        return new AddToPlaylistScreen(testDriver);
    }

    public AddCommentScreen clickComment() {
        clickMenuElementForFragment(commentItem(), AddCommentScreen.FRAGMENT_TAG);
        return new AddCommentScreen(testDriver);
    }

    public VisualPlayerElement toggleRepost() {
        repostItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public void clickStartStation() {
        startStation().click();
    }

    public StationHomeScreen clickOpenStation() {
        openStation().click();
        return new StationHomeScreen(testDriver);
    }

    private ViewElement openStation() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.stations_open_station)))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement addToPlaylistItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.add_to_playlist)))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement shareItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.share)))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement repostItem() {
        return findOnScreenElement(With.text(testDriver.getString(R.string.repost),
                                             testDriver.getString(R.string.unpost)))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement commentItem() {
        return getRootViewElement()
                .findOnScreenElement(With.textContaining("Comment"))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement startStation() {
        return getRootViewElement()
                .findOnScreenElement(With.text(testDriver.getString(R.string.stations_start_track_station)))
                .findAncestor(getRootViewElement(), With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement info() {
        return getRootViewElement().findOnScreenElement(With.text("Info"));
    }
}
