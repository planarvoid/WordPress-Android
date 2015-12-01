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
        return getRootMenuElements().get(0);
    }

    public ViewElement shareItem() {
        return getRootMenuElements().get(1);
    }

    public ViewElement repostItem() {
        return getRootMenuElements().get(2);
    }

    public ViewElement commentItem() {
        return container().findElement(With.textContaining("Comment"));
    }

    private ViewElement startStation() {
        return container().findElement(With.text(testDriver.getString(R.string.stations_start_track_station)));
    }

    private ViewElement info() {
        return container().findElement(With.text("Info"));
    }
}
