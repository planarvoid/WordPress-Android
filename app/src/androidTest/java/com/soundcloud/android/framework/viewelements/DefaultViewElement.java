package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.ViewFetcher;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.RecyclerViewElement;
import com.soundcloud.android.screens.elements.SlidingTabs;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.ToggleButton;

import java.util.List;

public final class DefaultViewElement extends ViewElement {
    private final Han testDriver;
    private final View view;
    private ViewFetcher viewFetcher;

    public DefaultViewElement(View view, Han driver) {
        if (view == null) {
            throw new IllegalArgumentException("viewElement cannot be null");
        }
        testDriver = driver;
        viewFetcher = new ViewFetcher(view, driver);
        this.view = view;
    }

    @Override
    public ViewElement findElement(With with) {
        return viewFetcher.findElement(with);
    }

    @Override
    public List<ViewElement> findElements(With with) {
        return viewFetcher.findElements(with);
    }

    @Override
    public void dragHorizontally(int n, int steps) {
        int[] xy = getLocation();
        testDriver.drag(Math.max(xy[0], 0),
                Math.max(Math.min(getScreenWidth(), xy[0] + n), 0),
                xy[1],
                xy[1],
                steps);
    }

    @Override
    public ViewElement getChildAt(int index) {
        return viewFetcher.getChildAt(index);
    }

    @Override
    public void click() {
        if (!isVisible()) {
            throw new ViewNotVisibleException();
        }
        Log.i("CLICKEVENT", String.format("Clicking at: %s", getClickPoint()));
        testDriver.clickOnView(view);
    }

    private String getClickPoint() {
        return String.format("%.02f, %.02f", view.getX() + view.getWidth()/2, view.getY() + view.getHeight()/2);
    }

    @Override
    public void longClick() {
        testDriver.clickLongOnView(view);
    }

    @Override
    public boolean isVisible() {
        return isShown() && hasVisibility() && hasDimensions() && isOnScreen();
    }

    @Override
    public boolean isAnimating() {
        final Animation animation = view.getAnimation();
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    @Override
    public ListElement toListView() {
        return new ListElement(view, testDriver);
    }

    @Override
    public RecyclerViewElement toRecyclerView() {
        return new RecyclerViewElement(view, testDriver);
    }

    @Override
    public SlidingTabs toSlidingTabs() {
        return new SlidingTabs(this);
    }

    @Override
    public boolean isEnabled() {
        return view.isEnabled();
    }

    @Override
    public boolean isChecked() {
        return ((ToggleButton) view).isChecked();
    }

    @Override
    public int getId() {
        return view.getId();
    }

    @Override
    public ViewParent getParent() {
        return view.getParent();
    }

    @Override
    public Class getViewClass() {
        return view.getClass();
    }

    @Override
    public ViewPager toViewPager() {
        return (ViewPager) view;
    }

    @Override
    public WebView toWebView() {
        return (WebView) view;
    }

    @Override
    public DownloadImageView toDownloadImageView() {
        return (DownloadImageView) view;
    }

    private boolean hasDimensions() {
        return getHeight() > 0 && getWidth() > 0;
    }

    @Override
    public int getHeight() {
        return view.getHeight();
    }

    @Override
    public int getWidth() {
        return view.getWidth();
    }

    @Override
    public int getTop() {
        return view.getTop();
    }

    @Override
    /* package */ View getView() { return view; }

    @Override
    /* package */ Han getTestDriver() { return testDriver; }

    private boolean isShown() {
        return view.isShown();
    }

    private boolean isOnScreen() {
        return getLocation()[0] >= 0 &&
                getLocation()[0] <= getScreenWidth() &&
                getLocation()[1] >= 0 &&
                getLocation()[1] <= getScreenHeight();
    }

    private int[] getLocation() {
        int[] locationOnScreen = new int[2];
        view.getLocationOnScreen(locationOnScreen);
        return locationOnScreen;
    }

    //TODO: Move this to Device class
    private int getScreenWidth() {
        return getDisplay().getWidth();
    }

    private int getScreenHeight() {
        return getDisplay().getHeight();
    }

    private Display getDisplay() {
        return testDriver.getDisplay();
    }

    private boolean hasVisibility() {
        return view.getVisibility() == View.VISIBLE;
    }
}
