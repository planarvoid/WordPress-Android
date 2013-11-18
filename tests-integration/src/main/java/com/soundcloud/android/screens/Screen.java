package com.soundcloud.android.screens;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public abstract class Screen {
    protected Han solo;
    protected Waiter waiter;

    public Screen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void pullToRefresh() {
        solo.swipeDownToRefresh();
        waiter.waitForTextToDisappear("Loading");
    }

    public void swipeLeft() {
        solo.swipeLeft();
    }

    public void swipeRight() {
        solo.swipeRight();
    }


    public boolean isVisible() {
        return getActivity().getSimpleName().equals(solo.getCurrentActivity().getClass().getSimpleName());
    }

    abstract protected Class getActivity();
}