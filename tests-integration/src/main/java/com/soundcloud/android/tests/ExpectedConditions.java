package com.soundcloud.android.tests;

import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.screens.elements.ViewElement;

import android.view.View;
import android.widget.TextView;

public class ExpectedConditions {

    private final ViewElement view;
    private final Waiter waiter;

    public ExpectedConditions(Waiter waiter, ViewElement view) {
        this.waiter = waiter;
        this.view = view;
    }

    public void toHaveText(String text) {
        waiter.waitForTextInView((TextView) view.getView(), text);
        assertEquals("Element should have text", text, view.getText());
    }

}
