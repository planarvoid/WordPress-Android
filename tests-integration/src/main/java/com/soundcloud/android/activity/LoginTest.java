package com.soundcloud.android.activity;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Start;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.ActivityInstrumentationTestCase2;

public class LoginTest extends ActivityInstrumentationTestCase2<Main> {
    private Han solo;

    public LoginTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        solo = new Han(getInstrumentation(), getActivity());
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
        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertText(R.string.authentication_log_in);

        solo.clearEditText(0);
        solo.enterText(0, IntegrationTestHelper.USERNAME);
        solo.enterText(1, IntegrationTestHelper.PASSWORD);

        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertDialogClosed(20 * 1000);
        solo.assertText(R.string.tab_stream);
    }

    public void testLoginWithWrongCredentials() {
        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertText(R.string.authentication_log_in);

        solo.clearEditText(0);
        solo.enterText(0, IntegrationTestHelper.USERNAME);
        solo.enterText(1, "wrong-password");

        solo.clickOnButton(solo.getString(R.string.btn_login));
        solo.assertText(R.string.authentication_login_error_password_message);
        solo.clickOnOK();
    }

    public void testLoginAndLogout() throws Exception {
        testLogin();
        solo.clickOnMenuItem(R.string.menu_settings);
        solo.clickOnText(R.string.pref_revoke_access);
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnOK();

        solo.assertText(R.string.authentication_log_in);
        assertTrue(solo.getCurrentActivity() instanceof Start);
    }
}
