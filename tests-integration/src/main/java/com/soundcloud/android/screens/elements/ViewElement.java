package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;

import android.content.Context;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class ViewElement {
    private final Han testDriver;
    private final View mView;

    public ViewElement(View element, Han driver) {
        testDriver = driver;
        mView = element;
    }

    public ViewElement(Han driver){
        testDriver = driver;
        mView = null;
    }

    public void click() {
        if( !isVisible() ) {
            throw new Error("View is not visible, cannot click it!");
        }
        testDriver.clickOnView(mView);
    }

    public void typeText(String text) {
        testDriver.typeText((EditText)mView, text);
    }
    public void clearText() {
        testDriver.clearEditText((EditText)mView);
    }

    public boolean isVisible(){
        return mView != null && isShown() && hasVisibility() && hasDimentions() && isOnScreen();
    }

    public String getText() {
        return ((TextView) mView).getText().toString();
    }

    public int getHeight() {
        return mView.getHeight();
    }

    public int getWidth() {
        return mView.getWidth();
    }

    public int[] getLocation() {
        int[] locationOnScreen = new int [2];
        mView.getLocationOnScreen(locationOnScreen);
        return locationOnScreen;
    }

    public View getView() {
        return mView;
    }

    public ViewElement findElement(int id) {
        //TODO parent!
        return testDriver.findElement(id);
    }

    public ViewElement findElement(Class <? extends View> viewClass) {
        return testDriver.findElement(viewClass);
    }

    public ListElement toList() {
        return new ListElement(mView, testDriver);
    }

    public int getId() {
        return mView.getId();
    }

    private boolean hasDimentions() {
        return getHeight() > 0 && getWidth() > 0 ;
    }
    private boolean isShown() {
        return mView.isShown();
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
        return mView.getVisibility() == View.VISIBLE;
    }

}