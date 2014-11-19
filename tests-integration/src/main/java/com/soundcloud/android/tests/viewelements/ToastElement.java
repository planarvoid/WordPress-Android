package com.soundcloud.android.tests.viewelements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

public class ToastElement {
    private final Han testDriver;

    public ToastElement(Han driver) {
        testDriver = driver;
    }

    private ViewElement toastMessage() {
        return testDriver.findElement(With.id(android.R.id.message));
    }

    public String getText() {
        return new TextElement(toastMessage()).getText();
    }
}
