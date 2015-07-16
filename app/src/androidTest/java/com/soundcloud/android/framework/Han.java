package com.soundcloud.android.framework;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;

import com.robotium.solo.By;
import com.robotium.solo.Condition;
import com.robotium.solo.Solo;
import com.soundcloud.android.framework.viewelements.DefaultViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension for {@link Solo}, to provider some cleaner assertions / driver logic.
 */
public class Han  {
    private static final int TOOLBAR_HEIGHT = 25;
    private static ViewFetcher viewFetcher;

    private final Solo solo;
    private final Instrumentation instrumentation;
    private Activity visibleActivity = new EmptyActivity();

    @Deprecated
    public Solo getSolo() {
        return solo;
    }

    public Han(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        solo = new Solo(instrumentation);
        viewFetcher = new ViewFetcher(this);

        setupActivityListener();
    }

    private void setupActivityListener() {
        IntentFilter filter = null;
        final Instrumentation.ActivityMonitor activityMonitor = instrumentation.addMonitor(filter, null, false);

        Runnable runnable = new Runnable() {
            public void run() {
                while (true) {
                    Activity activity = activityMonitor.waitForActivity();
                    if(activity != null && !activity.isFinishing()) {
                        visibleActivity = activity;
                        Log.i("ActivityMonitor:", visibleActivity.toString());
                    }
                }
            }
        };
        Thread thread = new Thread(runnable, "activityListenerThread");
        thread.start();
    }

    public ViewElement wrap(View view) {
        return new DefaultViewElement(view, this);
    }

    public ViewElement findElement(With findBy) {
        return viewFetcher.findElement(findBy);
    }

    public List<ViewElement> findElements(With with) {
        return viewFetcher.findElements(with);
    }

    public void typeText(EditText editText, String text) {
        solo.typeText(editText, text);
    }

    public void clearEditText(EditText editText) {
        solo.clearEditText(editText);
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

    public void clickOnActionBarHomeButton() {
        solo.clickOnActionBarHomeButton();
    }

    public void clickOnActionBarItem(int itemId) {
        final ArrayList<ActionMenuItemView> currentViews = solo.getCurrentViews(ActionMenuItemView.class);
        for (View view : currentViews){
            if (view.getId() == itemId){
                solo.clickOnView(view);
                break;
            }
        }
    }

    public ArrayList getViews(View view) {
        return solo.getViews(view);
    }

    public void clickOnView(View view) {
        solo.clickOnView(view);
    }

    public void clickLongOnView(View view) {
        solo.clickLongOnView(view);
    }

    public void clickOnButtonWithText(int textId) {
        solo.clickOnButton(getString(textId));
    }

    public String getString(int resId, Object... args) {
        return solo.getCurrentActivity().getString(resId, args);
    }

    public String getQuantityString(int resId, int quantity, Object... args) {
        return solo.getCurrentActivity().getResources().getQuantityString(resId, quantity, args);
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

    public void swipeRight() {
        swipeHorizontal(Solo.RIGHT);
    }


    public void swipeUp() {
        Point deviceSize = new Point();
        solo.getCurrentActivity().getWindowManager().getDefaultDisplay().getSize(deviceSize);

        final int screenWidth = deviceSize.x;
        final int screenHeight = deviceSize.y;

        drag(screenWidth / 4, screenWidth / 2, screenHeight - TOOLBAR_HEIGHT , screenHeight / 2, 20);
    }

    public void swipeDown() {
        Point deviceSize = new Point();
        solo.getCurrentActivity().getWindowManager().getDefaultDisplay().getSize(deviceSize);

        final int screenWidth = deviceSize.x;
        final int screenHeight = deviceSize.y;

        drag(screenWidth / 4, screenWidth / 4, screenHeight / 4, screenHeight / 2, 10);
    }

    public boolean isElementDisplayed(With matcher) {
        return viewFetcher.isElementDisplayed(matcher);
    }

    private void swipeHorizontal(int side) {
        // swipe at vertical center and from one horizontal side to the other
        swipeHorizontal(side, .8f, .5f);
    }

    private void swipeHorizontal(int side, float horizontalPosition, float verticalPosition){
        Point deviceSize = new Point();
        solo.getCurrentActivity().getWindowManager().getDefaultDisplay().getSize(deviceSize);

        final int screenWidth = deviceSize.x;
        final int screenHeight = deviceSize.y;

        // horizontal/vertical offset
        float x = screenWidth * horizontalPosition;
        float y = screenHeight * verticalPosition;

        //each ~50 pixels is one step
        final int steps = (int) x / 50;

        if (side == Solo.LEFT) {
            drag(x, 0, y, y, steps);
        } else if (side == Solo.RIGHT) {
            drag(0, x, y, y, steps);
        }
    }

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        log("dragging: (%.2f, %.2f) -> (%.2f, %.2f) count: %d", fromX, fromY, toX, toY, stepCount);
        solo.drag(fromX, toX, fromY, toY, stepCount);
    }

    public void clickOnView(With with) {
        List<ViewElement> views = findElements(with);
        if(!views.isEmpty()) {
            views.get(0).click();
        }
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
        return visibleActivity;
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

    public void scrollToBottom() {
        solo.scrollToBottom();
    }

    public void scrollToPosition(final RecyclerView recyclerView, final int position) {
        instrumentation.runOnMainSync(
                new Runnable() {
                    public void run() {
                        recyclerView.scrollToPosition(position);
                    }
                }
        );
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

    public void waitForDialogToOpen(long timeout) {
        solo.waitForDialogToOpen(timeout);
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

    public void setup() {
        With.setResources(instrumentation.getTargetContext().getResources());
    }

    public boolean scrollUp() {
        return solo.scrollUp();
    }

    public boolean scrollDown() {
        return solo.scrollDown();
    }

    public Display getDisplay() {
        return ((WindowManager) instrumentation.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    class EmptyActivity extends Activity {}
}
