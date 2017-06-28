package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;

public class NewForYouBucketElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public NewForYouBucketElement(Han testDriver,
                                  ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public boolean isOnScreen() {
        return wrapped.isOnScreen();
    }

    public SystemPlaylistScreen clickViewAll() {
        viewAll().click();

        return new SystemPlaylistScreen(testDriver);
    }

    private ViewElement viewAll() {
        return wrapped.findOnScreenElement(With.id(R.id.view_all));

    }
}
