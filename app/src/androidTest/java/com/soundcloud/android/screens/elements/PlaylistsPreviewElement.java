package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.PlaylistsScreen;

public class PlaylistsPreviewElement {

    private final Han testDriver;
    private final ViewElement wrapped;

    public PlaylistsPreviewElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public boolean isOnScreen() {
        return wrapped.isOnScreen();
    }

    public PlaylistsScreen click() {
        wrapped.click();
        return new PlaylistsScreen(testDriver);
    }

}
