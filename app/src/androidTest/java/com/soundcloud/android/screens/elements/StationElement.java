package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class StationElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public StationElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public String getTitle() {
        return new TextElement(wrapped.findElement(With.id(R.id.title))).getText();
    }

    public boolean isVisible() { return wrapped.isVisible(); }

    public VisualPlayerElement click() {
        wrapped.click();
        return new VisualPlayerElement(testDriver);
    }
}
