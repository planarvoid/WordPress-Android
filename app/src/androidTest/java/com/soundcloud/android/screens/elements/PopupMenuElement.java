package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.widget.TextView;

import java.util.List;

public class PopupMenuElement {

    protected final Han testDriver;
    private final Waiter waiter;

    public PopupMenuElement(Han testDriver) {
        this.testDriver = testDriver;
        waiter = new Waiter(testDriver);
        waiter.waitForElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    protected List<ViewElement> menuItems() {
        waiter.waitForElement(TextView.class);
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }

    protected ViewElement menuItem(With matcher) {
        waiter.waitForElement(TextView.class);
        return container().findElement(matcher);
    }

    protected ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    protected String getElementText(ViewElement viewElement) {
        return new TextElement(viewElement.findElement(With.className("android.widget.TextView"))).getText();
    }

}
