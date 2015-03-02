package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class PlaylistItemElement {
    private final ViewElement wrapped;

    public PlaylistItemElement(ViewElement wrapped) {
        this.wrapped = wrapped;
    }

    public String getTitle() {
        return getText(wrapped.findElement(With.id(R.id.list_item_subheader)));
    }

    public String getCreator() {
        return getText(wrapped.findElement(With.id(R.id.list_item_header)));
    }

    private String getText(ViewElement element) {
        return new TextElement(element).getText();
    }
}
