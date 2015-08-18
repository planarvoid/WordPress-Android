package com.soundcloud.android.utils;

import android.util.DisplayMetrics;

public class DisplayMetricsStub extends DisplayMetrics {

    public DisplayMetricsStub() {
        this(1920, 1080);
    }

    public DisplayMetricsStub(int width, int height) {
        super();
        this.widthPixels = width;
        this.heightPixels = height;
    }
}
