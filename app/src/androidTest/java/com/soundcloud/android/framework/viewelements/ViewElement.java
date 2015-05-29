package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public abstract class ViewElement {
    public abstract ViewElement findElement(With with);

    public abstract List<ViewElement> findElements(With with);

    public abstract void dragHorizontally(int n, int steps);

    public abstract ViewElement getChildAt(int index);

    public abstract void click();

    public abstract void longClick();

    public abstract boolean isEnabled();

    public abstract boolean isChecked();

    public abstract int getId();

    public abstract boolean isVisible();

    public abstract boolean isAnimating();

    public abstract int getHeight();

    public abstract int getWidth();

    public abstract int getTop();

    public abstract ListElement toListView();

    public abstract SlidingTabs toSlidingTabs();

    public abstract ViewParent getParent();

    public abstract Class getViewClass();

    public abstract ViewPager toViewPager();

    public abstract WebView toWebView();

    public abstract DownloadImageView toDownloadImageView();

    /* package */ abstract View getView();

    /* package */ abstract Han getTestDriver();
}
