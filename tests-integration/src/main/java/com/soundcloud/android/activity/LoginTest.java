package com.soundcloud.android.activity;


import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.tests.InstrumentationHelper;

import android.test.ActivityInstrumentationTestCase2;

public class LoginTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;

    public LoginTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        InstrumentationHelper.logOut(getInstrumentation());
        solo = new Solo(getInstrumentation(), getActivity());
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        super.tearDown();
    }

    public void testLogin() throws Exception {
        assertTrue(solo.waitForText("Log In"));

        solo.sendKey(Solo.DOWN);
        solo.sendKey(Solo.ENTER);
        assertTrue(solo.waitForText("Log In"));

        solo.clearEditText(0);
        solo.enterText(0, InstrumentationHelper.USERNAME);
        solo.enterText(1, InstrumentationHelper.PASSWORD);

        solo.clickOnButton("Log In");
        waitForDialogToClose(15000);
        assertTrue(solo.waitForText("Stream", 1, 5000));
    }

    public void testLoginAndLogout() throws Exception {
        testLogin();
        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Log Out");
        assertTrue(solo.waitForText("Clear user data?"));
        solo.clickOnButton("OK");

        assertTrue(solo.waitForText("Log In"));
        assertTrue(solo.getCurrentActivity() instanceof Main);
    }

    private void waitForDialogToClose(long timeout) {
        if (!solo.waitForDialogToClose(timeout)) {
            solo.takeScreenshot();
            throw new AssertionError("dialog did not close");
        }
    }
}
