package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.tests.with.With;

public abstract class Element {

    protected Han solo;
    protected Waiter waiter;

    public Element(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public boolean isVisible() {
        return solo.findElement(With.id(getRootViewId())).isVisible();
    }

    abstract protected int getRootViewId();
}
