package com.soundcloud.android.framework.viewelements;

import com.robotium.solo.Solo;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;

public class RadioButtonElement {
    protected final RadioButton view;
    private final Solo testDriver;

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
