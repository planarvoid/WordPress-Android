package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.TrackInfoScreen;

public class PlayerMenuElement extends PopupMenuElement {

    public PlayerMenuElement(Han solo) {
        super(solo);
    }

    public TrackInfoScreen clickInfo() {
        info().click();
        return new TrackInfoScreen(testDriver);
    }

    public AddToPlaylistScreen clickAddToPlaylist() {
        addToPlaylistItem().click();
        return new AddToPlaylistScreen(testDriver);
    }

    public AddCommentScreen clickComment() {
        commentItem().click();
        return new AddCommentScreen(testDriver);
    }

    public VisualPlayerElement toggleRepost() {
        repostItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public void clickStartStation() {
        startStation().click();
    }

    private ViewElement addToPlaylistItem() {
        return findElement(With.text(testDriver.getString(R.string.add_to_playlist)))
                .findAncestor(container(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement shareItem() {
        return findElement(With.text(testDriver.getString(R.string.share)))
                .findAncestor(container(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement repostItem() {
        return findElement(With.text(testDriver.getString(R.string.repost), testDriver.getString(R.string.unpost)))
                .findAncestor(container(), With.classSimpleName("ListMenuItemView"));
    }

    public ViewElement commentItem() {
        return container()
                .findElement(With.textContaining("Comment"))
                .findAncestor(container(), With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement startStation() {
        return container()
                .findElement(With.text(testDriver.getString(R.string.stations_start_track_station)))
                .findAncestor(container(), With.classSimpleName("ListMenuItemView"));
    }

    private ViewElement info() {
        return container().findElement(With.text("Info"));
    }
}
