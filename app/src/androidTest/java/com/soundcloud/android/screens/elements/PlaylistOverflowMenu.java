package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

public class PlaylistOverflowMenu extends PopupMenuElement {
    public PlaylistOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public VisualPlayerElement shuffle() {
        shuffleItem().click();
        return new VisualPlayerElement(testDriver);
    }

    public PlaylistDetailsScreen clickMakeAvailableOffline() {
        getMakeAvailableOfflineItem().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public ViewElement getMakeAvailableOfflineItem() {
        return findElement(With.text(testDriver.getString(R.string.make_offline_available)));
    }

    private ViewElement shuffleItem() {
        return findElement(With.text(testDriver.getString(R.string.shuffle_playlist)));
    }
}
