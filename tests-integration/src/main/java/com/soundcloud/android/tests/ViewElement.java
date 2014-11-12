package com.soundcloud.android.tests;


import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.UIView;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public interface ViewElement extends ElementWithText, UIView {
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

    int getHeight();

    int getWidth();

    int getTop();

    void clearText();

    String getText();

    ListElement toListView();

    SlidingTabs toSlidingTabs();

    ViewParent getParent();

    Class getViewClass();

    ViewPager toViewPager();

    WebView toWebView();
}
