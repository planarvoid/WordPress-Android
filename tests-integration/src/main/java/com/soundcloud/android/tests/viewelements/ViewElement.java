package com.soundcloud.android.tests.viewelements;

import com.robotium.solo.Solo;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public abstract class ViewElement {
    abstract public ViewElement findElement(With with);

    abstract public List<ViewElement> findElements(With with);

    abstract public void dragHorizontally(int n, int steps);

    abstract public ViewElement getChildAt(int index);

    abstract public void click();

    abstract public void longClick();

    abstract public boolean isEnabled();

    abstract public boolean isChecked();

    abstract public int getId();

    abstract public boolean isVisible();

    abstract public int getHeight();

    abstract public int getWidth();

    abstract public int getTop();

    abstract public ListElement toListView();

    abstract public SlidingTabs toSlidingTabs();

    abstract public ViewParent getParent();

    abstract public Class getViewClass();

    abstract public ViewPager toViewPager();

    abstract public WebView toWebView();

    abstract /* package */  View getView();

    abstract /* package */  Solo getTestDriver();
}
