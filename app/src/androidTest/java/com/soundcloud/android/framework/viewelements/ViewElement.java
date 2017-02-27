package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.Tabs;
import com.soundcloud.android.utils.Log;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import java.util.List;

public abstract class ViewElement {

    public abstract ViewElement findOnScreenElementWithPopulatedText(With id);

    protected static void log(String msg) {
        Log.i("ViewElement", msg);
    }

    public abstract ViewElement findOnScreenElement(With with);

    public abstract List<ViewElement> findOnScreenElements(With with);

    public abstract ViewElement findOnScreenElement(final With... withs);

    public abstract List<ViewElement> findOnScreenElements(final With... withs);

    public abstract ViewElement findElement(With with);

    public abstract ViewElement findAncestor(ViewElement root, With with);

    public abstract boolean isAncestorOf(ViewElement child);

    public abstract void dragHorizontally(int n, int steps);

    public abstract ViewElement getChildAt(int index);

    public abstract void click();

    public abstract void longClick();

    public abstract boolean isEnabled();

    public abstract boolean isChecked();

    public abstract int getId();

    public abstract CharSequence getContentDescription();

    public abstract boolean isOnScreen();

    public abstract boolean isFullyOnScreen();

    public abstract boolean hasVisibility();

    public abstract boolean dragFullyOnScreenVertical();

    public abstract boolean isAnimating();

    public abstract int getHeight();

    public abstract int getWidth();

    public abstract int getTop();

    public abstract int getGlobalTop();

    public abstract ListElement toListView();

    public abstract RecyclerViewElement toRecyclerView();

    public abstract Tabs toTabs();

    public abstract ViewParent getParent();

    public abstract Class getViewClass();

    public abstract ViewPager toViewPager();

    public abstract WebView toWebView();

    public abstract DownloadImageView toDownloadImageView();

    /* package */
    abstract View getView();

    /* package */
    abstract Han getTestDriver();

    abstract public List<ViewElement> getDirectChildren();

    public abstract boolean isElementOnScreen(With id);

    // This is temporary. Sorry.
    public abstract String debugOfflineTest();
}
