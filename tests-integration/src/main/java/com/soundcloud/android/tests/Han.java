package com.soundcloud.android.tests;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.robotium.solo.By;
import com.robotium.solo.Condition;
import com.robotium.solo.Solo;
import com.soundcloud.android.R;
import junit.framework.AssertionFailedError;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An extension for {@link Solo}, to provider some cleaner assertions / driver logic.
 */
public class Han  {
    private static final String TAG = Han.class.getSimpleName().toString();
    private static final int DEFAULT_TIMEOUT = 20 * 1000;
    private static ViewFetcher viewFetcher;

    private final Solo solo;

    public Solo getSolo() {
        return solo;
    }

    public Han(Instrumentation instrumentation) {
        solo = new Solo(instrumentation);
        viewFetcher = new ViewFetcher(solo);
    }

    public ViewElement findElement(int viewId) {
        return viewFetcher.findElement(viewId);
    }

    public void clickOnOK() {
        clickOnText(getString(android.R.string.ok));
    }

    public void clickOnPublish() {
        clickOnButtonResId(R.string.btn_publish);
    }

    public void clearTextInWebElement(By by) {
        solo.clearTextInWebElement(by);
    }

    public void clickOnText(int resId) {
        clickOnText(getString(resId));
    }

    public void clickOnText(String text) {
        solo.clickOnText(text);
    }

    public void clickOnText(String text, boolean scroll) {
        solo.clickOnText(text, 1, true);
    }

    public void sendKey(int key) {
        solo.sendKey(key);
    }

    public void clickOnView(View view) {
        assertNotNull(view);
        solo.clickOnView(view);
    }

    public void clickLongOnView(int resId) {
        View view = solo.getCurrentActivity().findViewById(resId);
        assertNotNull(view);
        solo.clickLongOnView(view);
    }

    public void assertText(int resId, Object... args) {
        final String text = getString(resId, args);
        assertTrue("Text '" + text + "' not found", solo.waitForText(Pattern.quote(text)));
    }

    public void assertVisibleText(String text, long timeout) {
        assertTrue(solo.waitForText(text, 0, timeout, false, true));
    }

    public void assertText(String text) {
        assertTrue("text " + text + " not found", solo.waitForText(text));
    }

    public void assertNoText(int resId, Object... args) {
        String text = getString(resId, args);
        assertFalse("Did not expect to find text: " + text, solo.searchText(Pattern.quote(text), true));
    }

    public <T extends Activity> T assertActivity(Class<T> a) {
        return assertActivity(a, DEFAULT_TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    public <T extends Activity> T assertActivity(Class<T> a, int timeout) {
        final boolean found = solo.waitForActivity(a.getSimpleName(), timeout);
        Activity activity = solo.getCurrentActivity();
        if (!found && !a.isAssignableFrom(activity.getClass())) {
            fail("Current activity is " + activity.getClass().getSimpleName() + ", expected " + a.getSimpleName());
        }
        return (T) activity;
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

    public void assertActivityFinished() {
        Activity a = solo.getCurrentActivity();
        assertNotNull("activity is null", a);
        assertTrue("Activity "+a+" not finished", a.isFinishing());
    }

    public void assertDialogClosed() {
        // TODO: replace with more intelligent checks
        //assertTrue(waitForDialogToClose(DEFAULT_TIMEOUT));
    }

    public void clickOnButtonResId(int resId) {
        solo.clickOnButton(getString(resId));
    }

    public String getString(int resId, Object... args) {
        return solo.getCurrentActivity().getString(resId, args);
    }

    public ListView getCurrentListView(){
        solo.waitForView(ListView.class);
        final ArrayList<ListView> currentListViews = solo.getCurrentViews(ListView.class);
        return currentListViews == null || currentListViews.isEmpty() ? null : currentListViews.get(0);
    }

    public GridView getCurrentGridView(){
        solo.waitForView(GridView.class);
        final ArrayList<GridView> currentGridViews = solo.getCurrentViews(GridView.class);
        return currentGridViews == null || currentGridViews.isEmpty() ? null : currentGridViews.get(0);
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

    public void enterTextId(int resId, String text) {
        View v = solo.getCurrentActivity().findViewById(resId);
        if (v instanceof EditText) {
            solo.enterText((EditText) v, text);
        } else fail("could not find edit text with id " + resId);
    }

    public void swipeHorizontal(int side) {
        swipeHorizontal(side, .5f);
    }

    public void swipeHorizontal(int side, float verticalPosition){
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

    public void dragViewHorizontally(View view, int n, int steps) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        drag(Math.max(xy[0], 0),
                Math.max(Math.min(getScreenWidth(), xy[0] + n), 0),
                xy[1],
                xy[1],
                steps);
    }

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        log("dragging: (%.2f, %.2f) -> (%.2f, %.2f) count: %d", fromX, fromY, toX, toY, stepCount);
        solo.drag(fromX, toX, fromY, toY, stepCount);
    }

    public void log(Object msg, Object... args) {
        Log.d(getClass().getSimpleName(), msg == null ? null : String.format(msg.toString(), args));
    }

    public void log(View view) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        log("View: " + view.getClass().getSimpleName() + " loc: " + Arrays.toString(xy));
    }

    public void clearEditText(EditText editText) {
        solo.clearEditText(editText);
    }

    public void waitForActivity(Class<? extends Activity> name) {
        waitForActivity(name.getSimpleName());
    }

    public void waitForActivity(Class<? extends Activity> name, int timeout) {
        solo.waitForActivity(name.getSimpleName(), timeout);
    }

    @Deprecated
    public void waitForActivity(String name) {
        assertTrue(String.format("timeout waiting for activity %s, current=%s",
                name, solo.getCurrentActivity()), solo.waitForActivity(name));
    }


    public View getView(int id) {
        View view = null;
        try {
            view = solo.getView(id);
        } catch(AssertionFailedError ignored) {

        }
        return view;
    }

    public <T extends View> boolean waitForView(View view) {
        return solo.waitForView(view);
    }

    public void clickOnButton(Integer resource) {
        solo.clickOnButton(getString(resource));
    }

    public View waitForViewId(int viewId, int timeout) {
        long endTime = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            Activity activity = getCurrentActivity();
            if (activity != null) {
                View view = activity.findViewById(viewId);
                if (view != null) {
                    return view;
                }
            }
        }
        fail("timeout waiting for view "+viewId);
        return null;
    }

    public View waitForViewId(int viewId, int timeout, boolean failIfNotFound) {
        long endTime = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            Activity activity = getCurrentActivity();
            if (activity != null) {
                View view = activity.findViewById(viewId);
                if (view != null) {
                    return view;
                }
            }
        }

        if (failIfNotFound) {
            fail("timeout waiting for view "+viewId);
        }

        return null;
    }


    public void enterText(int index, String text) {
        solo.enterText(index, text);
    }

    public void enterText(EditText editText, String text) {
        solo.enterText(editText, text);
    }

    public void finishOpenedActivities() {
        solo.finishOpenedActivities();
    }

    public void sleep(int time) {
        solo.sleep(time);
    }

    public <T extends View> T getView(Class<T> viewClass, int index) {
        T view = solo.getView(viewClass, index);
        assertNotNull(view);
        return view;
    }

    public void goBack() {
        solo.goBack();
    }

    public Activity getCurrentActivity() {
        return solo.getCurrentActivity();
    }

    public boolean isToggleButtonChecked(String text) {
        return solo.isToggleButtonChecked(text);
    }

    public boolean scrollListToTop(int index) {
        solo.waitForView(ListView.class);
        return solo.scrollListToTop(index);
    }

    public void scrollToBottom(AbsListView view) {
        solo.scrollListToBottom(view);
    }

    public void assertTextFound(String text, boolean onlyVisible) {
        assertTrue("Text " + text + " not found", solo.searchText(text, onlyVisible));
    }
    public boolean searchText(String text, boolean onlyVisible) {
        return solo.searchText(text, onlyVisible);
    }

    public boolean searchTextWithoutScrolling(String text){
        return solo.searchText(text,0,false,true);
    }

    public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll) {
        return solo.searchText(text, minimumNumberOfMatches, false);
    }

    public void typeText(EditText editText, String text) {
        solo.typeText(editText, text);
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

    public void scrollToItem(int item) {
        solo.scrollListToLine(0, item - 1);
    }

    public List<TextView> clickInList(int item) {
        return solo.clickInList(item);
    }

    public void waitForDialogToClose(long timeout) {
        solo.waitForDialogToClose(timeout);
    }

    public boolean waitForText(String text, int minimumNumberOfMatches, long timeout, boolean scroll) {
        return solo.waitForText(text, minimumNumberOfMatches, timeout, scroll);
    }

    public void openSystemMenu() {
        solo.sendKey(Solo.MENU);
    }

    public boolean waitForFragmentByTag(String fragment_tag, int timeout) {
        return solo.waitForFragmentByTag(fragment_tag, timeout);
    }

    public void clickInList(int line, int index) {
        solo.clickInList(line, index);
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

    private class ViewClicker {
        private final Solo testDriver;

        public ViewClicker(Solo driver) {
            testDriver = driver;
        };

    }
}
