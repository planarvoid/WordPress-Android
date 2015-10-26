package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class Tabs {

    private final ViewElement tabLayout;

    public Tabs(ViewElement parentView) {
        tabLayout = parentView.getChildAt(0);
    }

    public ViewElement getTabWithText(String tabText) {
        return tabLayout.findElement(With.text(tabText));
    }

    public ViewElement getTabAt(int index) {
        return tabLayout.getChildAt(index).getChildAt(0);
    }
}
