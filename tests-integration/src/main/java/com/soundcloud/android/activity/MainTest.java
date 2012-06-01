package com.soundcloud.android.activity;


import com.jayway.android.robotium.solo.Solo;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;


@Suppress
public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;

    public MainTest() {
        super(Main.class);
    }

    @Override public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override public void tearDown() throws Exception {
        super.tearDown();
        if (solo != null) {
            solo.finishOpenedActivities();
        }
    }

    public void testLogin() throws Exception {
        solo.waitForText("Log In");

        solo.sendKey(Solo.DOWN);
        solo.sendKey(Solo.ENTER);
        solo.waitForText("Log In");

        solo.clearEditText(0);
        solo.enterText(0, "aeffle");
        solo.enterText(1, "m0nk3yz");

        solo.clickOnButton("Log In");
        assertTrue(solo.waitForText("Stream", 1, 10000));
    }
}
