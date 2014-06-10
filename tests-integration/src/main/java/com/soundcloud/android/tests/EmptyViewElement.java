package com.soundcloud.android.tests;

import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;
import android.view.ViewParent;

import java.util.List;

public class EmptyViewElement implements ViewElement {
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
    public void typeText(String text) {
        throw new ViewNotFoundException();
    }

    @Override
    public void clearText() {
        throw new ViewNotFoundException();
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int[] getLocation() {
        return new int[] {0,0};
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
}
