package com.soundcloud.android.activity;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
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
        solo.clickOnButton(solo.getString(R.string.btn_signup));
        assertTrue(solo.waitForText(solo.getString(R.string.authentication_sign_up)));

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButton(solo.getString(R.string.btn_signup));
        assertTrue(solo.waitForDialogToClose(20 * 1000));
        assertTrue(solo.waitForText(solo.getString(R.string.authentication_add_info_msg)));

        // username (max 25 characters)
        solo.enterText(0, uuid.substring(0, 24).replace("-", ""));
        solo.clickOnButton(solo.getString(R.string.btn_save));

        assertTrue(solo.waitForDialogToClose(20 * 1000));

        // Tour
        assertTrue(solo.waitForText(solo.getString(R.string.tour_start_welcome)));

        solo.clickOnButton(solo.getString(R.string.btn_done));

        // Find Friends
        assertTrue(solo.waitForText(solo.getString(R.string.suggested_users_msg)));

        solo.clickOnButton(solo.getString(R.string.btn_done));

        assertTrue(solo.waitForText(solo.getString(R.string.tab_stream)));
    }
}
