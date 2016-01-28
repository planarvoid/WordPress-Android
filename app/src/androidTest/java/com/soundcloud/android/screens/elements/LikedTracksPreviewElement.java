package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.TrackLikesScreen;

public class LikedTracksPreviewElement {

    private final Han testDriver;
    private final ViewElement wrapped;

    public LikedTracksPreviewElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public boolean isOnScreen() {
        return wrapped.isOnScreen();
    }

    public TrackLikesScreen click() {
        wrapped.click();
        return new TrackLikesScreen(testDriver);
    }

    public DownloadImageViewElement downloadElement() {
        return new DownloadImageViewElement(testDriver, wrapped.findOnScreenElement(With.id(R.id.item_download_state)));
    }
}
