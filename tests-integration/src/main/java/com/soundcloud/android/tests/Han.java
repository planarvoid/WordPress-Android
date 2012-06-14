package com.soundcloud.android.tests;

import static junit.framework.Assert.assertTrue;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;

import android.app.Activity;
import android.app.Instrumentation;

import java.util.regex.Pattern;

public class Han extends Solo {
    private static final long DEFAULT_TIMEOUT = 20 * 1000;

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

    public void clickOnMenuItem(int resId) {
        clickOnMenuItem(getString(resId));
    }

    public void assertText(int resId, Object... args) {
        assertTrue(waitForText(Pattern.quote(getString(resId, args))));
    }

    public void assertText(String text) {
        assertTrue(waitForText(text));
    }

    public void assertActivity(Class<? extends Activity> a) {
        assertTrue(waitForActivity(a.getSimpleName()));
    }

    public void assertActivityFinished() {
        assertTrue(getCurrentActivity().isFinishing());
    }

    public void assertDialogClosed() {
        assertDialogClosed(DEFAULT_TIMEOUT);
    }

    public void assertDialogClosed(long timeout) {
        assertTrue(waitForDialogToClose(timeout));
    }

    public void clickOnButtonResId(int resId) {
        super.clickOnButton(getString(resId));
    }

    public String getString(int resId, Object... args) {
        return getCurrentActivity().getString(resId, args);
    }
}
