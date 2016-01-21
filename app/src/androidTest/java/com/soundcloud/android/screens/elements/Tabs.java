package com.soundcloud.android.screens.elements;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.view.CustomFontTabLayout;

import java.util.List;

public class Tabs {
    final Han testDriver;

    public Tabs(Han testDriver) {
        this.testDriver = testDriver;
        final Waiter waiter = new Waiter(testDriver);
        waiter.waitForElement(With.className(CustomFontTabLayout.class.getName()));
        waiter.waitForElementCondition(new TabsVisibleCondition(this));
    }

    public ViewElement getTabWith(With matcher) {
        return container().findOnScreenElement(matcher);
    }

    public ViewElement getTabAt(int index) {
        return tabs().get(index);
    }

    public boolean isVisible() {
        return tabs().size() == 4;
    }

    private List<ViewElement> tabs() {
        return container().getChildren().get(0).getChildren();
    }

    private ViewElement container() {
        return testDriver.findOnScreenElement(With.className(CustomFontTabLayout.class.getName()));
    }

    private class TabsVisibleCondition implements Condition {

        private final Tabs tabs;

        public TabsVisibleCondition(Tabs tabs) {
            this.tabs = tabs;
        }
        @Override
        public boolean isSatisfied() {
            return tabs.isVisible();
        }
    }
}
