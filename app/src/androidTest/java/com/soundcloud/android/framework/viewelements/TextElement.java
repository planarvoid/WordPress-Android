package com.soundcloud.android.framework.viewelements;

import android.view.View;
import android.widget.TextView;

public class TextElement {
    protected final ViewElement viewElement;
    protected final View view;

    public TextElement(ViewElement element) {
        this.viewElement = element;
        this.view = element.getView();
    }

    public String getText() {
        if (view instanceof TextView) {
            return ((TextView) view).getText().toString();
        }
        throw new UnsupportedOperationException("View is not a text element: " + view);
    }

    public void click() {
        this.viewElement.click();
    }
}
