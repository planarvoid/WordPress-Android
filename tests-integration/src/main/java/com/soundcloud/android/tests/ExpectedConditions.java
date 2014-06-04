package com.soundcloud.android.tests;

import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.screens.elements.ViewElement;

public class ExpectedConditions {

    private final ViewElement viewElement;
    private final Waiter waiter;

    public ExpectedConditions(Waiter waiter, ViewElement element) {
        this.waiter = waiter;
        this.viewElement = element;
    }

    public void toHaveText(String text) {
        waiter.waitForTextInView(viewElement, text);
        assertEquals("Element should have text", text, viewElement.getText());
    }

}
