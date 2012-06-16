package com.soundcloud.android.tests;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.Display;

import java.util.regex.Pattern;

/**
 * An extension for {@link Solo}, to provider some cleaner assertions / driver logic.
 */
public class Han extends Solo {
    private static final long DEFAULT_TIMEOUT = 20 * 1000;
    private static final int SWIPE_SLEEP = 1000;

    public Han(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);
    }

    @SuppressWarnings("UnusedDeclaration")
    public Han(Instrumentation instrumentation) {
        super(instrumentation);
    }

    public void clickOnOK() {
        clickOnText(getString(android.R.string.ok));
    }

    public void clickOnNext() {
        clickOnButtonResId(R.string.btn_next);
    }

    public void clickOnText(int resId) {
        clickOnText(getString(resId));
    }

    public void clickOnView(int resId) {
        clickOnView(getCurrentActivity().findViewById(resId));
    }

    public void clickLongOnView(int resId) {
        clickLongOnView(getCurrentActivity().findViewById(resId));
    }


    public void clickOnMenuItem(int resId) {
        clickOnMenuItem(getString(resId));
    }

    public void assertText(int resId, Object... args) {
        assertTrue(waitForText(Pattern.quote(getString(resId, args))));
    }

    public void assertVisibleText(int resId, Object... args) {
        assertTrue(waitForText(Pattern.quote(getString(resId, args)), 0, DEFAULT_TIMEOUT, false, true));
    }

    public void assertText(String text) {
        assertTrue(waitForText(text));
    }

    public void assertNoText(int resId, Object... args) {
        String text = getString(resId, args);
        assertFalse("Did not expect to find text: "+text, searchText(Pattern.quote(text), true));
    }

    @SuppressWarnings("unchecked")
    public <T extends Activity> T assertActivity(Class<T> a) {
        assertTrue(waitForActivity(a.getSimpleName()));
        return (T) getCurrentActivity();
    }

    public void assertActivityFinished() {
        assertTrue(getCurrentActivity().isFinishing());
    }

    public void assertDialogClosed() {
        // TODO: replace with more intelligent checks
        //assertTrue(waitForDialogToClose(DEFAULT_TIMEOUTe));
    }

    public void clickOnButtonResId(int resId) {
        super.clickOnButton(getString(resId));
    }

    public String getString(int resId, Object... args) {
        return getCurrentActivity().getString(resId, args);
    }

    public void swipeLeft() {
        swipe(Solo.LEFT);
        sleep(SWIPE_SLEEP);
    }

    public void swipeRight() {
        swipe(Solo.RIGHT);
        sleep(SWIPE_SLEEP);
    }

    public void swipe(int side) {
        Display display = getCurrentActivity().getWindowManager().getDefaultDisplay();

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


    public void logoutViaSettings() {
        clickOnMenuItem(R.string.menu_settings);
        clickOnText(R.string.pref_revoke_access);
        assertText(R.string.menu_clear_user_title);
        clickOnOK();
        assertText(R.string.authentication_log_in);
    }
}
