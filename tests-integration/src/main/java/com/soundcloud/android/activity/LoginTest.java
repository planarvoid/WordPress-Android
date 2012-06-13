package com.soundcloud.android.activity;


import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Start;
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
        solo.clickOnButton(solo.getString(R.string.btn_login));
        assertTrue(solo.waitForText(solo.getString(R.string.authentication_log_in)));

        solo.clearEditText(0);
        solo.enterText(0, InstrumentationHelper.USERNAME);
        solo.enterText(1, InstrumentationHelper.PASSWORD);

        solo.clickOnButton(solo.getString(R.string.btn_login));
        assertTrue(solo.waitForDialogToClose(20 * 1000));
        assertTrue(solo.waitForText(solo.getString(R.string.tab_stream), 1, 15000));
    }

    public void testLoginWithWrongCredentials() {
        solo.clickOnButton(solo.getString(R.string.btn_login));
        assertTrue(solo.waitForText(solo.getString(R.string.authentication_log_in)));

        solo.clearEditText(0);
        solo.enterText(0, InstrumentationHelper.USERNAME);
        solo.enterText(1, "wrong-password");

        solo.clickOnButton(solo.getString(R.string.btn_login));
        assertTrue(solo.waitForText(solo.getString(R.string.authentication_login_error_password_message)));
        solo.clickOnButton("OK");
    }

    public void testLoginAndLogout() throws Exception {
        testLogin();
        solo.clickOnMenuItem(solo.getString(R.string.menu_settings));
        solo.clickOnText(solo.getString(R.string.pref_revoke_access));
        assertTrue(solo.waitForText(solo.getString(R.string.menu_clear_user_title)));
        solo.clickOnButton("OK");

        assertTrue(solo.waitForText(solo.getString(R.string.authentication_log_in)));
        assertTrue(solo.getCurrentActivity() instanceof Start);
    }
}
