package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.widget.EditText;

public class EditTextElement extends TextElement {
    private final Han testDriver;

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
