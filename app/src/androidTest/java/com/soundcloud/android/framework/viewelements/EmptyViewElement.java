package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.RecyclerViewElement;
import com.soundcloud.android.screens.elements.SlidingTabs;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public final class EmptyViewElement extends ViewElement {

    private String selector;

    public EmptyViewElement(String selector) {
        this.selector = selector;
    }

    @Override
    public ViewElement findElement(With with) {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public List<ViewElement> findElements(With with) {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public void dragHorizontally(int n, int steps) {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public ViewElement getChildAt(int index) {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public void click() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public void longClick() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public boolean isAnimating() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public int getHeight() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public int getWidth() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public int getTop()  {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public ListElement toListView() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public RecyclerViewElement toRecyclerView() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public SlidingTabs toSlidingTabs() {
        throw new ViewNotFoundException(selector);
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
        throw new ViewNotFoundException(selector);
    }

    @Override
    public ViewParent getParent() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public Class getViewClass() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public ViewPager toViewPager() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public WebView toWebView() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    public DownloadImageView toDownloadImageView() {
        throw new ViewNotFoundException(selector);
    }

    @Override
    /* package */ View getView() { throw new ViewNotFoundException(selector); }

    @Override
    /* package */ Han getTestDriver() { throw new ViewNotFoundException(selector); }
}
