package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;

public class SingleContentSelectionCardElement extends Element {

    public SingleContentSelectionCardElement(Han solo, With matcher) {
        super(solo, matcher);
    }

    public SystemPlaylistScreen clickCard() {
        getRootViewElement().findElement(With.id(R.id.single_card_title)).click();
        return new SystemPlaylistScreen(testDriver);
    }
}
