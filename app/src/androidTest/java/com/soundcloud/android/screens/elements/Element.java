package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public abstract class Element {

    private final With matcher;
    protected Han testDriver;
    protected Waiter waiter;

    public Element(Han solo, With matcher) {
        this.testDriver = solo;
        this.waiter = new Waiter(solo);
        this.matcher = matcher;
    }

    public boolean isVisible() {
        return getRootViewElement().isVisible();
    }

    public boolean isFullyVisible() {
        return getRootViewElement().isFullyVisible();
    }

    public ViewElement getRootViewElement() {
        return testDriver.findElement(matcher);
    }
}
