package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public abstract class Element {

    protected Han solo;
    protected Waiter waiter;

    public Element(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
        waiter.waitForElement(getRootViewId());
    }

    abstract protected int getRootViewId();

}
