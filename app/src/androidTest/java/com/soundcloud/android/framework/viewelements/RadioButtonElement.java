package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.widget.RadioButton;

public class RadioButtonElement {
    protected final RadioButton view;
    private final Han testDriver;

    public RadioButtonElement(ViewElement element) {
        this.view = (RadioButton) element.getView();
        this.testDriver = element.getTestDriver();
    }

    public boolean isChecked() {
        return view.isChecked();
    }

    public void click() {
        testDriver.clickOnView(view);
    }
}
