package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class StationElement {
    private final ViewElement wrapped;

    public StationElement(ViewElement wrapped) {
        this.wrapped = wrapped;
    }

    public String getTitle() {
        return new TextElement(wrapped.findElement(With.id(R.id.title))).getText();
    }

    public boolean isVisible() { return wrapped.isVisible(); }
}
