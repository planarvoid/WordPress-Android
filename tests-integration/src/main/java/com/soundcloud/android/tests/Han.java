package com.soundcloud.android.tests;

import static com.google.common.collect.Collections2.filter;
import static junit.framework.Assert.assertTrue;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.robotium.solo.By;
import com.robotium.solo.Condition;
import com.robotium.solo.Solo;
import com.soundcloud.android.tests.with.With;

import android.app.Activity;
import android.app.Instrumentation;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An extension for {@link Solo}, to provider some cleaner assertions / driver logic.
 */
public class Han  {
    private static ViewFetcher viewFetcher;

    private final Solo solo;

    @Deprecated
    public Solo getSolo() {
        return solo;
    }

    public Han(Instrumentation instrumentation) {
        solo = new Solo(instrumentation);
        viewFetcher = new ViewFetcher(solo);
    }

    public ViewElement wrap(View view) {
        return new DefaultViewElement(view, solo);
    }

    public ToastElement getToast() { return new ToastElement(this); }

    public ViewElement findElement(With findBy) {
        return viewFetcher.findElement(findBy);
    }

    public List<ViewElement> findElements(With with) {
        return viewFetcher.findElements(with);
    }

    public void clickOnText(String text) {
        findElement(With.text(text)).click();
    }

    public void clickOnText(int stringId) {
        String text = getString(stringId);
        findElement(With.text(text)).click();
    }

    public void clearTextInWebElement(By by) {
        solo.clearTextInWebElement(by);
    }

    public void sendKey(int key) {
        solo.sendKey(key);
    }

    @Deprecated
    public void assertText(int resId, Object... args) {
        final String text = getString(resId, args);
        assertTrue("Text '" + text + "' not found", solo.waitForText(Pattern.quote(text)));
    }

    @Deprecated
    public void assertText(String text) {
        assertTrue("text " + text + " not found", solo.waitForText(text));
    }

    public void clickOnActionBarHomeButton() {
        try {
            solo.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    solo.clickOnActionBarHomeButton();
                }
            });
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not click on action bar home button on UI Thread", throwable);
        }
    }

    public void clickOnActionBarItem(int itemId) {
        solo.clickOnActionBarItem(itemId);
    }

    public void clickOnButtonWithText(int textId) {
        solo.clickOnButton(getString(textId));
    }

    public String getString(int resId, Object... args) {
        return solo.getCurrentActivity().getString(resId, args);
    }

    @Deprecated
    public GridView getCurrentGridView(){
        solo.waitForView(GridView.class);
        final ArrayList<GridView> currentGridViews = solo.getCurrentViews(GridView.class);
        return currentGridViews == null || currentGridViews.isEmpty() ? null : currentGridViews.get(0);
    }

    @Deprecated
    public ListView getCurrentListView(){
        solo.waitForView(ListView.class);
        final ArrayList<ListView> currentListViews = solo.getCurrentViews(ListView.class);
        return currentListViews == null || currentListViews.isEmpty() ? null : currentListViews.get(0);
    }

    public void swipeLeft() {
        swipeHorizontal(Solo.LEFT);
    }

    public void swipeLeft(float verticalPosition) {
        swipeHorizontal(Solo.LEFT, verticalPosition);
    }

    public void swipeRight() {
        swipeHorizontal(Solo.RIGHT);
    }

    public void swipeRight(float verticalPosition){
        swipeHorizontal(Solo.RIGHT, verticalPosition);
    }

    public void swipeDown() {
        Display display = solo.getCurrentActivity().getWindowManager().getDefaultDisplay();

        final int screenHeight = display.getHeight();
        final int screenWidth = display.getWidth();

        drag(screenWidth / 4, screenWidth / 4, screenHeight / 4, screenHeight / 2, 10);
    }

    public boolean isElementDisplayed(With matcher) {
        return viewFetcher.isElementDisplayed(matcher);
    }

    private void swipeHorizontal(int side) {
        swipeHorizontal(side, .5f);
    }

    private void swipeHorizontal(int side, float verticalPosition){
        Display display = solo.getCurrentActivity().getWindowManager().getDefaultDisplay();

        final int screenHeight = display.getHeight();
        final int screenWidth = display.getWidth();

        // center of the screen
        float x = screenWidth * verticalPosition;
        float y = screenHeight * verticalPosition;

        //each ~50 pixels is one step
        final int steps = (int) x / 50;

        if (side == Solo.LEFT) {
            drag(x, 0, y, y, steps);
        } else if (side == Solo.RIGHT) {
            drag(x, screenWidth, y, y, steps);
        }
    }

    public int getScreenWidth() {
        Display display = solo.getCurrentActivity().getWindowManager().getDefaultDisplay();
        return display.getWidth();
    }

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        log("dragging: (%.2f, %.2f) -> (%.2f, %.2f) count: %d", fromX, fromY, toX, toY, stepCount);
        solo.drag(fromX, toX, fromY, toY, stepCount);
    }


    @Deprecated
    public void clickOnButton(final Integer resource) {
        ArrayList<ViewElement> buttonsWithText = findButtonsWithText(getString(resource));
        if(!buttonsWithText.isEmpty()) {
            buttonsWithText.get(0).click();
        }
    }

    private ArrayList<ViewElement> findButtonsWithText(final String text) {
        return Lists.newArrayList(filter(findElements(With.className(Button.class)), new Predicate<ViewElement>() {
                    @Override
                    public boolean apply(ViewElement viewElement) {
                        return viewElement.findElement(With.text(text)).getText().equals(text);
                    }
               })
        );
    }

    public void finishOpenedActivities() {
        solo.finishOpenedActivities();
    }

    @Deprecated
    public void sleep(int time) {
        solo.sleep(time);
    }

    public void goBack() {
        solo.goBack();
    }

    public Activity getCurrentActivity() {
        return solo.getCurrentActivity();
    }

    public boolean scrollListToTop(int index) {
        solo.waitForView(ListView.class);
        return solo.scrollListToTop(index);
    }

    public void scrollListToLine(int line) {
        solo.scrollListToLine(0, line);
    }

    public void scrollToBottom(AbsListView view) {
        solo.scrollListToBottom(view);
    }

    public boolean searchText(String text, boolean onlyVisible) {
        return solo.searchText(text, onlyVisible);
    }

    public boolean waitForWebElement(By by) {
        return solo.waitForWebElement(by);
    }

    public void typeTextInWebElement(By by, String text) {
        solo.typeTextInWebElement(by, text);
    }

    public void clickOnWebElement(By by) {
        solo.clickOnWebElement(by);
    }

    public void takeScreenshot(String name) {
        solo.takeScreenshot(name);
    }

    public boolean waitForCondition(Condition condition, int timeout) {
        return solo.waitForCondition(condition, timeout);
    }

    public void waitForDialogToClose(long timeout) {
        solo.waitForDialogToClose(timeout);
    }

    public void openSystemMenu() {
        solo.sendKey(Solo.MENU);
    }

    public boolean waitForFragmentByTag(String fragment_tag, int timeout) {
        return solo.waitForFragmentByTag(fragment_tag, timeout);
    }

    public boolean isKeyboardShown() {
        InputMethodManager inputMethodManager = (InputMethodManager)  solo.getCurrentActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focusedView = solo.getCurrentActivity().getCurrentFocus();

        if (focusedView == null) {
            return false;
        }

        boolean canHideKeyboard = inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        if (canHideKeyboard) {
            inputMethodManager.showSoftInput(focusedView,  InputMethodManager.SHOW_IMPLICIT);
        }
        return canHideKeyboard;
    }

    private void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }
}
