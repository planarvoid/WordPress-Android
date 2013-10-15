package com.soundcloud.android.screens;

import com.soundcloud.android.tests.Han;

public class Screen {
    protected Han solo;

    public Screen(Han solo) {
        this.solo = solo;
    }

    public void swipeRight(){
        solo.swipeRight();
    }

    public void swipeLeft(){
        solo.swipeLeft();
    }

    public void pullToRefresh() {
        solo.swipeDownToRefresh();
    }
}