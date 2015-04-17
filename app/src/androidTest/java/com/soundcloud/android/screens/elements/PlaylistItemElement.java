package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

public class PlaylistItemElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public PlaylistItemElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public String getTitle() {
        return getText(wrapped.findElement(With.id(R.id.list_item_subheader)));
    }

    public String getCreator() {
        return getText(wrapped.findElement(With.id(R.id.list_item_header)));
    }

    public PlaylistDetailsScreen click() {
        wrapped.click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistItemOverflowMenu clickOverflow() {
        wrapped
                .findElement(With.id(R.id.overflow_button))
                .click();
        return new PlaylistItemOverflowMenu(testDriver);
    }

    private String getText(ViewElement element) {
        return new TextElement(element).getText();
    }
}
