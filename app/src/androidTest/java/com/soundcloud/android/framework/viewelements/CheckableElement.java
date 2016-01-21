package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.view.View;
import android.widget.Checkable;

public class CheckableElement {
    protected final Checkable view;
    private final Han testDriver;

    public CheckableElement(ViewElement element) {
        this.view = (Checkable) element.getView();
        this.testDriver = element.getTestDriver();
    }

    public boolean isChecked() {
        return view.isChecked();
    }

    public void click() {
        testDriver.clickOnView((View) view);
    }
}
