package com.soundcloud.android.tests;


import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.activity.Main;

import android.test.ActivityInstrumentationTestCase2;


public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;

    public MainTest() {
        super("com.soundcloud.android", Main.class);
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
