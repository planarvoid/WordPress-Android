package com.soundcloud.android.tests;

import android.widget.Toast;

/**
 * Created by slawomirsmiechura on 06/06/14.
 */
public class ToastElement {
    private final Han testDriver;

    public ToastElement(Han driver) {
        testDriver = driver;
    };

    private ViewElement toastMessage() {
        return testDriver.findElement(android.R.id.message);
    }

    public String getMessage() {
        return toastMessage().getText();
    }


}
