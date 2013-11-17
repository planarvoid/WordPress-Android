package com.soundcloud.android.screens;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.test.ActivityInstrumentationTestCase2;

public class Screen {
    protected Han solo;
    protected Waiter waiter;

    public Screen(ActivityInstrumentationTestCase2 testCas2) {
        this(new Han(testCas2.getInstrumentation(), testCas2.getActivity()));
    }

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
}