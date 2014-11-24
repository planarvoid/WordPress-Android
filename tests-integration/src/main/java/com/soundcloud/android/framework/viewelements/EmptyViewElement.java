package com.soundcloud.android.framework.viewelements;

import com.robotium.solo.Solo;
import com.soundcloud.android.framework.screens.elements.ListElement;
import com.soundcloud.android.framework.screens.elements.SlidingTabs;
import com.soundcloud.android.framework.with.With;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public final class EmptyViewElement extends ViewElement {
    @Override
    public ViewElement findElement(With with) {
        throw new ViewNotFoundException();
    }

    @Override
    public List<ViewElement> findElements(With with) {
        throw new ViewNotFoundException();
    }

    @Override
    public void dragHorizontally(int n, int steps) {
        throw new ViewNotFoundException();
    }

    @Override
    public ViewElement getChildAt(int index) {
        throw new ViewNotFoundException();
    }

    @Override
    public void click() {
        throw new ViewNotFoundException();
    }

    @Override
    public void longClick() {
        throw new ViewNotFoundException();
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public int getHeight() {
        throw new ViewNotFoundException();
    }

    @Override
    public int getWidth() {
        throw new ViewNotFoundException();
    }

    @Override
    public int getTop()  {
        throw new ViewNotFoundException();
    }

    @Override
    public ListElement toListView() {
        throw new ViewNotFoundException();
    }

    @Override
    public SlidingTabs toSlidingTabs() {
        throw new ViewNotFoundException();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public int getId() {
        throw new ViewNotFoundException();
    }

    @Override
    public ViewParent getParent() {
        throw new ViewNotFoundException();
    }

    @Override
    public Class getViewClass() {
        throw new ViewNotFoundException();
    }

    @Override
    public ViewPager toViewPager() {
        throw new ViewNotFoundException();
    }

    @Override
    public WebView toWebView() {
        throw new ViewNotFoundException();
    }

    @Override
    /* package */ View getView() { throw new ViewNotFoundException(); }

    @Override
    /* package */ Solo getTestDriver() { throw new ViewNotFoundException(); }
}
