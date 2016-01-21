package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class ToastElement {
    private final Han testDriver;

    public ToastElement(Han driver) {
        testDriver = driver;
    }

    private ViewElement toastMessage() {
        return testDriver.findOnScreenElement(With.id(android.R.id.message));
    }

    public String getText() {
        return new TextElement(toastMessage()).getText();
    }
}
