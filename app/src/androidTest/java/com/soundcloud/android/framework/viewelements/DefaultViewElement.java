package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.ViewFetcher;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.Tabs;

import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.ToggleButton;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public final class DefaultViewElement extends ViewElement {
    private final Han testDriver;
    private final View view;
    private ViewFetcher viewFetcher;
    private Waiter waiter;

    public DefaultViewElement(View view, Han driver) {
        waiter = new Waiter(driver);
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
    public ViewElement findAncestor(ViewElement root, With with) {
        return viewFetcher.findAncestor(root.getView(), with);
    }

    @Override
    // We are making the assumption that the direct children are likely
    // to be the matching ones and therefore perform a breadth first search.
    public boolean isAncestorOf(ViewElement child) {
        final Deque<View> queue = new LinkedList<>();

        final View root = this.view;
        final View expectedChild = child.getView();

        queue.add(root);
        while (!queue.isEmpty()) {
            final View current = queue.pop();

            if (current != root && current.equals(expectedChild)) {
                return true;
            }

            if (current instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) current;
                for (int i = 0, count = group.getChildCount(); i < count; i++) {
                    queue.add(group.getChildAt(i));
                }
            }
        }
        return false;
    }

    @Override
    public boolean isElementDisplayed(With matcher) {
        return viewFetcher.isElementDisplayed(matcher);
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
            waiter.waitForElementToBeVisible(this);
            if (!isVisible()) {
                throw new ViewNotVisibleException();
            }
        }
        Log.i("CLICKEVENT", String.format("View rect: %s", getRect().flattenToString()));
        Log.i("CLICKEVENT", String.format("Clicking at: %s", getClickPoint()));
        testDriver.clickOnScreen(getVisibleRect().exactCenterX(), getVisibleRect().exactCenterY()) ;
    }

    private String getClickPoint() {
        return String.format("%d, %d", getVisibleRect().centerX(), getVisibleRect().centerY());
    }

    private Rect getVisibleRect(){
        Rect visibleRect = new Rect();
        if(view.getGlobalVisibleRect(visibleRect) == true) {
            if(visibleRect.intersect(getRect())){
                return visibleRect;
            }
            return getRect();
        }
        return new Rect();
    }

    @Override
    public boolean isFullyVisible() {
        Rect viewRect = getRect();
        Log.i("CLICKEVENT", String.format("View rect: %s", getRect().flattenToString()));
        return getVisibleRect().contains(viewRect.left, viewRect.top, viewRect.right, viewRect.bottom);
    }

    private Rect getScreenRect() {
        return new Rect(0,0, getScreenWidth(), getScreenHeight());
    }

    private Rect getRect() {
        return new Rect(getLocation()[0], getLocation()[1], getLocation()[0] + view.getWidth(), getLocation()[1]+ view.getHeight());
    }

    @Override
    public void longClick() {
        testDriver.clickLongOnView(view);
    }

    @Override
    public boolean isVisible() {
        return !getVisibleRect().isEmpty() && isShown() && hasVisibility() && hasDimensions() && isOnScreen();
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
    public Tabs toTabs() {
        return new Tabs(testDriver);
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

    @Override
    public List<ViewElement> getChildren() {
        return viewFetcher.getChildren();
    }

    private boolean isShown() {
        return view.isShown();
    }

    private boolean isOnScreen() {
        return getRect().intersect(getScreenRect());
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
