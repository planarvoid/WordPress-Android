package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.widget.TextView;

import java.util.List;

public class PopupMenuElement extends Element {

    public PopupMenuElement(Han testDriver) {
        super(testDriver, With.className("android.widget.PopupWindow$PopupViewContainer"));
        waiter.waitForElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    protected List<ViewElement> getRootMenuElements() {
        waiter.waitForElement(TextView.class);
        return getRootViewElement().findOnScreenElements(With.classSimpleName("ListMenuItemView"));
    }

    protected ViewElement findOnScreenElement(With matcher) {
        waiter.waitForElement(TextView.class);
        return getRootViewElement().findOnScreenElement(matcher);
    }

    protected String getElementText(ViewElement viewElement) {
        return new TextElement(viewElement.findOnScreenElement(With.id(android.R.id.title))).getText();
    }

}
