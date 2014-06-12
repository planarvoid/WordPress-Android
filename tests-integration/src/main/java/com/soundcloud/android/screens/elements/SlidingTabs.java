package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class SlidingTabs {
    private final ViewElement tabsStrip;

    public SlidingTabs(ViewElement parentView) {
        tabsStrip = parentView.getChildAt(0);
    }

    public ViewElement getTabWithText(String tabText) {
        return tabsStrip.findElement(With.text(tabText));
    }

    public ViewElement getTabAt(int index) {
        return tabsStrip.getChildAt(index);
    }
}
