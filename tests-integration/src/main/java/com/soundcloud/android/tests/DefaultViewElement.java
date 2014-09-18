package com.soundcloud.android.tests;

import com.robotium.solo.Solo;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.tests.with.With;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

public class DefaultViewElement implements ViewElement {
    private final Solo testDriver;
    private final View view;
    private ViewFetcher viewFetcher;

    public DefaultViewElement(View view, Solo driver) {
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
        testDriver.clickOnView(view);
    }

    @Override
    public void longClick() {
        testDriver.clickLongOnView(view);
    }

    @Override
    public void typeText(String text) {
        testDriver.typeText((EditText) view, text);
    }

    @Override
    public void clearText() {
        testDriver.clearEditText((EditText) view);
    }

    @Override
    public boolean isVisible() {
        return isShown() && hasVisibility() && hasDimensions() && isOnScreen();
    }

    @Override
    public String getText() {
        if (view instanceof TextView) {
            return ((TextView) view).getText().toString();
        } else {
            return "";
        }
    }


    @Override
    public ListElement toListView() {
        return new ListElement(view, testDriver);
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

    private boolean hasDimensions() {
        return getHeight() > 0 && getWidth() > 0;
    }

    private int getHeight() {
        return view.getHeight();
    }

    private int getWidth() {
        return view.getWidth();
    }

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
        return ((WindowManager) testDriver.getCurrentActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    private boolean hasVisibility() {
        return view.getVisibility() == View.VISIBLE;
    }
}
