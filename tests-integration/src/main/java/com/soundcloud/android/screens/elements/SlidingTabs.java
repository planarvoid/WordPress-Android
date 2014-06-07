package com.soundcloud.android.screens.elements;

import com.robotium.solo.Solo;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class SlidingTabs {
    private final Solo testDriver;
    private final ViewElement tabsStrip;

    public SlidingTabs(ViewElement parentView, Solo driver) {
        testDriver = driver;
        tabsStrip = parentView.getChildAt(0);
    }

    public ViewElement getTabWithText(String tabText) {
        return tabsStrip.findElement(With.text(tabText));
    }

    public ViewElement getTabAt(int index) {
        return tabsStrip.getChildAt(index);
    }
}
