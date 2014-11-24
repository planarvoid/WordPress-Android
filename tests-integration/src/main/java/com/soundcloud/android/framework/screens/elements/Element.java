package com.soundcloud.android.framework.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.with.With;

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
