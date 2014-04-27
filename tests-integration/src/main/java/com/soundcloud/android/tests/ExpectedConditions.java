package com.soundcloud.android.tests;

import static junit.framework.Assert.assertEquals;

import android.view.View;
import android.widget.TextView;

public class ExpectedConditions {
    private final View mView;
    private final Waiter mWaiter;

    public ExpectedConditions(Waiter waiter, View view) {
        mWaiter = waiter;
        mView = view;
    }

    public void toHaveText(String text) {
        mWaiter.waitForTextInView((TextView) mView, text);
        assertEquals("Element should have text", text, ((TextView) mView).getText());
    }

}
