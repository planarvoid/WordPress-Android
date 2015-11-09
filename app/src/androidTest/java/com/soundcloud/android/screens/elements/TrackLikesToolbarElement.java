package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;

public class TrackLikesToolbarElement extends ToolBarElement {

    public TrackLikesToolbarElement(Han solo) {
        super(solo);
    }

    public LikesOverflowMenu clickOverflowButton() {
        overflowButton().click();
        return new LikesOverflowMenu(testDriver);
    }
}
