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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

public class ViewElement {
    private final Solo testDriver;
    private final View view;
    private ViewFetcher viewFetcher;

    public ViewElement(View element, Solo driver) {
        testDriver = driver;
        view = element;
        viewFetcher = new ViewFetcher(view, driver);
    }

    public ViewElement(Solo driver){
        testDriver = driver;
        view = null;
    }
    public ViewElement findElement(With with) {
        return viewFetcher.findElement(with);
    }

    public List<ViewElement> findElements(With with) {
        return viewFetcher.findElements(with);
    }

    public void dragHorizontally(int n, int steps) {
        int[] xy = getLocation();
        testDriver.drag(Math.max(xy[0], 0),
                Math.max(Math.min(getScreenWidth(), xy[0] + n), 0),
                xy[1],
                xy[1],
                steps);
    }

    public ViewElement getChildAt(int index) {
        return viewFetcher.getChildAt(index);
    }

    public void click() {
        if( !isVisible() ) {
            throw new Error("View is not visible, cannot click it!");
        }
        testDriver.clickOnView(view);
    }

    public void longClick() {
        testDriver.clickLongOnView(view);
    }

    public void typeText(String text) {
        testDriver.typeText((EditText) view, text);
    }

    public void clearText() {
        testDriver.clearEditText((EditText) view);
    }

    public boolean isVisible(){
        return view != null && isShown() && hasVisibility() && hasDimensions() && isOnScreen();
    }

    public String getText() {
        if (view instanceof TextView) {
          return ((TextView) view).getText().toString();
        }  else {
          return "";
        }
    }

    public int getHeight() {
        return view.getHeight();
    }

    public int getWidth() {
        return view.getWidth();
    }

    public int[] getLocation() {
        int[] locationOnScreen = new int [2];
        view.getLocationOnScreen(locationOnScreen);
        return locationOnScreen;
    }

    public View getView() {
        return view;
    }

    public ListElement toListView() {
        return new ListElement(view, testDriver);
    }

    public SlidingTabs toSlidingTabs() {
        return new SlidingTabs(this, testDriver);
    }

    public boolean isEnabled() {
        return view.isEnabled();
    }

    public boolean isChecked() {
        return ((ToggleButton)view).isChecked();
    }

    public int getId() {
        return view.getId();
    }

    public boolean isTextView() {
        return (TextView.class.isAssignableFrom(view.getClass()));
    }

    public ViewParent getParent() {
        return view.getParent();
    }

    public Class getViewClass() {
        return view.getClass();
    }

    public ViewPager toViewPager() {
        return (ViewPager)view;
    }

    private boolean hasDimensions() {
        return getHeight() > 0 && getWidth() > 0 ;
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
