package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
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
        return getRootViewElement().findElements(With.classSimpleName("ListMenuItemView"));
    }

    protected ViewElement findElement(With matcher) {
        waiter.waitForElement(TextView.class);
        return getRootViewElement().findElement(matcher);
    }

    protected String getElementText(ViewElement viewElement) {
        return new TextElement(viewElement.findElement(With.id(android.R.id.title))).getText();
    }

}
