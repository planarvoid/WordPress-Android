package com.soundcloud.android.tests;

import com.soundcloud.android.tests.by.With;

public class ToastElement {
    private final Han testDriver;

    public ToastElement(Han driver) {
        testDriver = driver;
    };

    private ViewElement toastMessage() {
        return testDriver.findElement(With.id(android.R.id.message));
    }

    public String getMessage() {
        return toastMessage().getText();
    }


}
