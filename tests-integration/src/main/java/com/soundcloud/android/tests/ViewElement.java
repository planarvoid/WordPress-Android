package com.soundcloud.android.tests;


import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;


import java.util.List;

public interface ViewElement extends ElementWithText {
    ViewElement findElement(With with);

    List<ViewElement> findElements(With with);

    void dragHorizontally(int n, int steps);

    ViewElement getChildAt(int index);

    void click();

    void longClick();

    void typeText(String text);

    boolean isEnabled();

    boolean isChecked();

    int getId();

    boolean isVisible();

    void clearText();

    String getText();

    ListElement toListView();

    SlidingTabs toSlidingTabs();

    ViewParent getParent();

    Class getViewClass();

    ViewPager toViewPager();
}
