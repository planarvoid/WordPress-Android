package com.soundcloud.android.tests;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.jayway.android.robotium.solo.By;
import com.jayway.android.robotium.solo.Condition;
import com.jayway.android.robotium.solo.Solo;
import com.jayway.android.robotium.solo.WebElement;
import com.soundcloud.android.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.view.Display;
import android.view.View;
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
    private static final int DEFAULT_TIMEOUT = 20 * 1000;
    private static final int SWIPE_SLEEP = 1000;

    private final Solo solo;

    public Han(Instrumentation instrumentation, Activity activity) {
        solo = new Solo(instrumentation, activity);
    }

    @SuppressWarnings("UnusedDeclaration")
    public Han(Instrumentation instrumentation) {
        solo = new Solo(instrumentation);
    }

    public void clickOnOK() {
        clickOnText(getString(android.R.string.ok));
    }

    public void clickOnDone() {
        clickOnButtonResId(R.string.btn_done);
    }

    public void clickOnPublish() {
        clickOnButtonResId(R.string.btn_publish);
    }

    public WebElement getWebElement(By by, int index) {
        return solo.getWebElement(by, index);
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

    public void clickOnView(int resId) {
        solo.waitForView(resId);
        clickOnView(solo.getCurrentActivity().findViewById(resId));
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

    public void clickOnMenuItem(int resId) {
        solo.clickOnMenuItem(getString(resId));
    }

    public void assertText(int resId, Object... args) {
        final String text = getString(resId, args);
        assertTrue("Text '" + text + "' not found", solo.waitForText(Pattern.quote(text)));
    }

    public void assertVisibleTextId(int resId, Object... args) {
        assertTrue(solo.waitForText(Pattern.quote(getString(resId, args)), 0, DEFAULT_TIMEOUT, false, true));
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

    public void performClick(InstrumentationTestCase test, int resId) throws Throwable {
        final View view = getView(resId);
        assertNotNull("view is null", view);
        test.runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.performClick();
            }
        });
        solo.sleep(500);
    }


    public String getString(int resId, Object... args) {
        return solo.getCurrentActivity().getString(resId, args);
    }

    public List<ListView> getCurrentListViews(){
        return solo.getCurrentViews(ListView.class);
    }

    public AbsListView getCurrentListView(){
        final ArrayList<AbsListView> currentListViews = solo.getCurrentViews(AbsListView.class);
        return currentListViews == null || currentListViews.isEmpty() ? null : currentListViews.get(0);
    }

    public GridView getCurrentGridView(){
        final ArrayList<GridView> currentGridViews = solo.getCurrentViews(GridView.class);
        return currentGridViews == null || currentGridViews.isEmpty() ? null : currentGridViews.get(0);
    }


    public ArrayList<TextView> clickInList(int line){
        return solo.clickInList(line);
    }

    public ArrayList<TextView> clickInList(int line, int listIndex) {
        return solo.clickInList(line, listIndex);
    }

    public void swipeLeft() {
        swipe(Solo.LEFT);
        solo.sleep(SWIPE_SLEEP);
    }

    public void swipeRight() {
        swipe(Solo.RIGHT);
        solo.sleep(SWIPE_SLEEP);
    }

    public void enterTextId(int resId, String text) {
        View v = solo.getCurrentActivity().findViewById(resId);
        if (v instanceof EditText) {
            solo.enterText((EditText) v, text);
        } else fail("could not find edit text with id " + resId);
    }

    public void swipe(int side) {
        Display display = solo.getCurrentActivity().getWindowManager().getDefaultDisplay();

        final int screenHeight = display.getHeight();
        final int screenWidth = display.getWidth();

        // center of the screen
        float x = screenWidth / 2.0f;
        float y = screenHeight / 2.0f;

        final int steps = 1;
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

    public int getScreenHeight() {
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
        View view = solo.getView(id);
        assertNotNull("view is null", view);
        return view;
    }

    public <T extends View> boolean waitForView(View view) {
        return solo.waitForView(view);
    }

    public void clickOnButton(String text) {
        solo.clickOnButton(text);
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

    public void assertCurrentActivity(String message, String name) {
        solo.assertCurrentActivity(message, name);
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


    /**
     * Tell the CI/development machine (i.e. maven build process) to take a screenshot
     * @see <a href="https://github.com/rtyley/android-screenshot-lib/blob/master/celebrity/src/main/java/com/github/rtyley/android/screenshot/celebrity/Screenshots.java">
     *     android-screenshot-lib
     *     </a>
     */
    public void poseForScreenshot() {
        poseForScreenshotWithKeyValueString("");
    }

    public void poseForScreenshot(String name) {
        poseForScreenshotWithKeyValue("name", name);
    }

    public boolean scrollListToTop(int index) {
        return solo.scrollListToTop(index);
    }

    public void clickOnActionBarItem(int itemId) {
        solo.clickOnActionBarItem(itemId);
    }

    private void poseForScreenshotWithKeyValue(String key, String value) {
        poseForScreenshotWithKeyValueString(key + "=" + value);
    }

    private void poseForScreenshotWithKeyValueString(String keyValueString) {
        /* Note that the log message can not be blank, otherwise it won't register with logcat. */
        Log.d("screenshot_request", "{" + keyValueString + "}");

        /* Wait for the development machine to take the screenshot (can take about 900ms) */
        solo.sleep(1000);
    }

    public void assertTextFound(String text, boolean onlyVisible) {
        assertTrue("Text " + text + " not found", solo.searchText(text, onlyVisible));
    }
    public boolean searchText(String text, boolean onlyVisible) {
        return solo.searchText(text, onlyVisible);
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

    public boolean waitForDialogToOpen(long timeout) {
        return solo.waitForDialogToOpen(timeout);
    }

    public void takeScreenshot(String name) {
        solo.takeScreenshot(name);
    }

    public boolean waitForCondition(Condition condition, int timeout) {
        return solo.waitForCondition(condition, timeout);
    }
}

