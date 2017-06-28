package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;

public class MultiContentSelectionCardElement extends Element {

    public MultiContentSelectionCardElement(Han solo, With matcher) {
        super(solo, matcher);
    }

    public SystemPlaylistScreen clickFirstPlaylist() {
        testDriver.scrollToItem(With.id(R.id.selection_playlists_carousel));
        getRootViewElement().findElement(With.id(R.id.selection_playlists_carousel)).findElement(With.id(R.id.title)).click();
        return new SystemPlaylistScreen(testDriver);
    }

}
