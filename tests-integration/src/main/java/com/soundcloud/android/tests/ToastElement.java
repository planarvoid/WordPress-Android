package com.soundcloud.android.tests;

import com.soundcloud.android.tests.with.With;

public class ToastElement implements ElementWithText {
    private final Han testDriver;
    private final Waiter waiter;

    ToastElement(Han driver) {
        testDriver = driver;
        waiter = new Waiter(testDriver);
    }

    private ViewElement toastMessage() {
        return testDriver.findElement(With.id(android.R.id.message));
    }

    public String getText() {
        return toastMessage().getText();
    }
}
