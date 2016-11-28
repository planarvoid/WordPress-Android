package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.DebugHelper;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.widget.TextView;

import java.util.List;

public class PopupMenuElement extends Element {

    private static final String DROP_DOWN_CLS_NAME = "android.support.v7.widget.MenuPopupWindow$MenuDropDownListView";

    public PopupMenuElement(Han testDriver) {
        super(testDriver, With.className(DROP_DOWN_CLS_NAME));
        waiter.waitForElement(With.className(DROP_DOWN_CLS_NAME));
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
        return new TextElement(viewElement.findOnScreenElement(With.id(android.support.v7.appcompat.R.id.title))).getText();
    }

    protected boolean clickMenuElementForFragment(ViewElement viewElement, String fragmentTAg) {
        final int MAX_ATTEMPTS = 5;

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            // Clicking on the menu would sometimes fail silently.
            // We enforce a retry here.
            DebugHelper.log("clickMenuElementForFragment -> Try #" + i);
            viewElement.click();
            if (waiter.waitForFragmentByTag(fragmentTAg)) {
                return true;
            }
        }
        return false;
    }
}
