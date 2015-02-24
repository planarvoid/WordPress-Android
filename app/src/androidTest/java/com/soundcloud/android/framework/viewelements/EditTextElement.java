package com.soundcloud.android.framework.viewelements;

import com.robotium.solo.Solo;

import android.widget.EditText;

public class EditTextElement extends TextElement {
    private final Solo testDriver;

    public EditTextElement(ViewElement element) {
        super(element);
        this.testDriver = element.getTestDriver();
    }

    public void typeText(String text) {
        testDriver.typeText((EditText) view, text);
    }

    public void clearText() {
        testDriver.clearEditText((EditText) view);
    }
}
