package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;

public class TrackItemMenuElement extends PopupMenuElement {

    public TrackItemMenuElement(Han solo) {
        super(solo);
    }

    public void toggleLike() {
        menuItems().get(0).click();
    }

    public void clickAdToPlaylist() {
        menuItems().get(1).click();
    }
}
