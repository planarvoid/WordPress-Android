package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.ViewFetcher;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.Tabs;

import android.content.res.Resources;
import android.content.res.TypedArray;
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

public class DefaultViewElement extends ViewElement {
    private final View view;
    private final Han testDriver;
    private ViewFetcher viewFetcher;
    protected Waiter waiter;

    public DefaultViewElement(View view, Han driver) {
        if (view == null) {
            throw new IllegalArgumentException("viewElement cannot be null");
        }
        this.view = view;
        this.testDriver = driver;
        waiter = new Waiter(driver);
        viewFetcher = new ViewFetcher(view, driver);
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
    public ViewElement findElement(final With... withs) {
        return viewFetcher.findElement(withs);
    }

    @Override
    public List<ViewElement> findElements(final With... withs) {
        return viewFetcher.findElements(withs);
    }

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
        testDriver.clickOnScreen(getVisibleRect().exactCenterX(), getVisibleRect().exactCenterY());
    }

    private String getClickPoint() {
        return String.format("%f, %f", getVisibleRect().exactCenterX(), getVisibleRect().exactCenterY());
    }

    private Rect getVisibleRect() {
        Rect visibleRect = new Rect();
        if (view.getGlobalVisibleRect(visibleRect)) {
            if (visibleRect.intersect(getRect())) {
                return visibleRect;
            }
            return getRect();
        }
        return new Rect();
    }

    @Override
    public boolean dragIntoFullVerticalVisibility() {
        if (!viewCanFitVerticallyOnScreen()) {
            return false;
        }

        Rect visibleRect = getVisibleRect();
        Rect viewRect = getRect();

        if (isFullyVisible(visibleRect, viewRect)) {
            return true;
        }

        // top is cut off
        if (viewRect.top < visibleRect.top) {
            int viewBottom = visibleRect.bottom - 1; // account for zero-index
            int heightOfNonVisibleSection = visibleRect.top - viewRect.top;

            testDriver.scrollVertical(viewBottom, viewBottom + heightOfNonVisibleSection);
        }

        // bottom is cut off
        if (visibleRect.bottom < viewRect.bottom) {
            int viewBottom = visibleRect.bottom - 1; // account for zero-index
            int heightOfNonVisibleSection = viewRect.bottom - visibleRect.bottom;

            testDriver.scrollVertical(viewBottom, viewBottom - heightOfNonVisibleSection);
        }

        return true;
    }

    private boolean viewCanFitVerticallyOnScreen() {
        return getHeight() <= getAvailableScreenHeight();
    }

    private int getAvailableScreenHeight() {
        return getScreenHeight() - getStatusBarHeight() - getActionBarHeight() - getNavBarHeight();
    }

    private int getActionBarHeight() {
        final TypedArray styledAttributes = testDriver.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return actionBarHeight;
    }

    private int getStatusBarHeight() {
        // assume all devices have a status bar and are only ever in portait mode
        Resources resources = testDriver.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getNavBarHeight() {
        // assume all devices have a nav bar and are only ever in portait mode
        Resources resources = testDriver.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private boolean isFullyVisible(Rect visibleRect, Rect viewRect) {
        Log.i("CLICKEVENT", String.format("View rect: %s", viewRect.flattenToString()));
        return visibleRect.contains(viewRect.left, viewRect.top, viewRect.right, viewRect.bottom);
    }

    @Override
    public boolean isFullyVisible() {
        return isFullyVisible(getVisibleRect(), getRect());
    }

    private Rect getScreenRect() {
        return new Rect(0, 0, getScreenWidth(), getScreenHeight());
    }

    private Rect getRect() {
        return new Rect(getLocation()[0], getLocation()[1], getLocation()[0] + view.getWidth(), getLocation()[1] + view.getHeight());
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
    public CharSequence getContentDescription() {
        return view.getContentDescription();
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
    /* package */ View getView() {
        return view;
    }

    @Override
    /* package */ Han getTestDriver() {
        return testDriver;
    }

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
