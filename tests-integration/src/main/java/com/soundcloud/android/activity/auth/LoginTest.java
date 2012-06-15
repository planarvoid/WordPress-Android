package com.soundcloud.android.activity.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.auth.FacebookWebFlow;
import com.soundcloud.android.activity.auth.Recover;
import com.soundcloud.android.activity.auth.Start;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.webkit.WebView;

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
        solo.assertDialogClosed();
        solo.assertText(R.string.tab_stream);
    }

    @Suppress
    public void testLoginWithFacebook() throws Exception {
        solo.clickOnButtonResId(R.string.authentication_log_in_with_facebook);
        solo.assertDialogClosed();
        solo.assertActivity(FacebookWebFlow.class);

        solo.takeScreenshot();

        WebView webView = ((FacebookWebFlow) solo.getCurrentActivity()).getWebView();
        assertTrue("got url:"+webView.getUrl(), webView.getUrl().startsWith("http://m.facebook.com/login.php"));
        assertEquals("Facebook", webView.getTitle());
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

    public void testRecoverPassword() throws Exception {
        solo.clickOnText(R.string.authentication_I_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.enterText(0, "some-email@example.com");
        solo.clickOnOK();

        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
        solo.assertActivity(Start.class);
    }

    public void testRecoverPasswordNoInput() throws Exception {
        solo.clickOnText(R.string.authentication_I_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.clickOnOK();
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }
}
