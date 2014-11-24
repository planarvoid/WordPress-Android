package com.soundcloud.android.framework;

import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;

public class ExpectedConditions {

    private final ViewElement viewElement;
    private final Waiter waiter;

    public ExpectedConditions(Waiter waiter, ViewElement element) {
        this.waiter = waiter;
        this.viewElement = element;
    }

    public void toHaveText(String text) {
        waiter.waitForElement(new TextElement(viewElement), text);
        assertEquals("Element should have text", text, new TextElement(viewElement).getText());
    }

}
