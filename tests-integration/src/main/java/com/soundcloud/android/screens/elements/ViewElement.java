package com.soundcloud.android.screens.elements;

import com.robotium.solo.Solo;
import com.soundcloud.android.tests.Han;

import android.content.Context;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class ViewElement {
    private final Solo testDriver;
    private final View view;

    public ViewElement(View element, Solo driver) {
        testDriver = driver;
        view = element;
    }

    public ViewElement(Solo driver){
        testDriver = driver;
        view = null;
    }

    public void click() {
        if( !isVisible() ) {
            throw new Error("View is not visible, cannot click it!");
        }
        testDriver.clickOnView(view);
    }

    public void typeText(String text) {
        testDriver.typeText((EditText) view, text);
    }
    public void clearText() {
        testDriver.clearEditText((EditText) view);
    }

    public boolean isVisible(){
        return view != null && isShown() && hasVisibility() && hasDimentions() && isOnScreen();
    }

    public String getText() {
        if (view == null) return "";
        return ((TextView) view).getText().toString();
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

    public int getId() {
        return view.getId();
    }

    private boolean hasDimentions() {
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