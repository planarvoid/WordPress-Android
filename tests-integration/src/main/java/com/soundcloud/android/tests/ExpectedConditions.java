package com.soundcloud.android.tests;

import static junit.framework.Assert.assertEquals;

import android.view.View;
import android.widget.TextView;

public class ExpectedConditions {

    private final View view;
    private final Waiter waiter;

    public ExpectedConditions(Waiter waiter, View view) {
        this.waiter = waiter;
        this.view = view;
    }

    public void toHaveText(String text) {
        waiter.waitForTextInView((TextView) view, text);
        assertEquals("Element should have text", text, ((TextView) view).getText());
    }

}
