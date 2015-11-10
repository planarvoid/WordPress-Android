package com.soundcloud.android.screens.elements;

import android.support.design.widget.TabLayout;
import android.view.View;
import android.widget.ImageView;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Tabs {
    private final Han testDriver;
    private Waiter waiter;

    public Tabs(Han testDriver) {
        this.testDriver = testDriver;
        waiter = new Waiter(testDriver);
        waiter.waitForElement(With.classSimpleName(TabLayout.class.getSimpleName()));
        waiter.waitForElementCondition(new TabsVisibleCondition(this));
    }

    public ViewElement getTabWithText(String tabText) {
        return container().findElement(With.text(tabText));
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
        return testDriver.findElement(With.classSimpleName(TabLayout.class.getSimpleName()));
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
