package com.soundcloud.android.activity;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.tests.InstrumentationHelper;

import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;

import java.util.UUID;

public class SignupTest extends ActivityInstrumentationTestCase2<Main> {
    private Solo solo;

    public SignupTest() {
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

    @FlakyTest
    public void testSignup() throws Exception {
        solo.clickOnButton("Sign Up");
        assertTrue(solo.waitForText("Sign Up"));

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButton("Sign Up");
        waitForDialogToClose(60 * 1000);
        assertTrue(solo.waitForText("Almost done"));

        // username (max 25 characters)
        solo.enterText(0, uuid.substring(0, 24).replace("-", ""));
        solo.clickOnButton("Save");

        waitForDialogToClose(60 * 1000);

        // Tour
        assertTrue(solo.waitForText("Welcome to SoundCloud"));

        solo.clickOnButton("Done");

        // Find Friends
        assertTrue(solo.waitForText("Thanks for joining SoundCloud"));

        solo.clickOnButton("Done");

        assertTrue(solo.waitForText("Stream"));
        // TODO assert db state etc is gone
    }

    private void waitForDialogToClose(long timeout) {
        if (!solo.waitForDialogToClose(timeout)) {
            solo.takeScreenshot();
            throw new AssertionError("dialog did not close (timeout="+timeout+")");
        }
    }
}
