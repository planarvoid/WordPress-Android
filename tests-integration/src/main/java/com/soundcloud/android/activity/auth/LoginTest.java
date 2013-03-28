package com.soundcloud.android.activity.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.FlakyTest;
import android.webkit.WebView;
import android.widget.EditText;


public class LoginTest extends ActivityTestCase<Onboard> {
    public LoginTest() {
        super(Onboard.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.setUp();
        solo.sleep(500);
    }

    public void testLogin() throws Exception {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.assertText(R.string.authentication_log_in);

        EditText userField = (EditText) solo.getView(R.id.txt_email_address);

        solo.clearEditText(userField);
        solo.enterText(userField, IntegrationTestHelper.USERNAME);
        solo.enterText((EditText) solo.getView(R.id.txt_password), IntegrationTestHelper.PASSWORD);

        solo.clickOnDone();
        solo.assertDialogClosed();
        solo.assertText(R.string.side_menu_stream);
    }

    @FlakyTest
    public void testLoginWithFacebookWebFlow() throws Throwable {
        if (FacebookSSO.isSupported(getInstrumentation().getTargetContext())) {
            log("Facebook SSO is available, not testing WebFlow");
            return;
        }
        solo.performClick(this, R.id.facebook_btn);
        solo.assertDialogClosed();
        WebView webView = solo.assertActivity(FacebookWebFlow.class).getWebView();
        assertNotNull(webView);
        int i = 0;
        while (webView.getUrl() == null && i++ < 40) {
            solo.sleep(500);
        }
        assertNotNull("FB request timed out", webView.getUrl());
        assertTrue("got url:" + webView.getUrl(), webView.getUrl().contains("facebook.com"));
    }

    public void testLoginWithWrongCredentials() {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.assertText(R.string.authentication_log_in);

        EditText userField = (EditText) solo.getView(R.id.txt_email_address);
        solo.clearEditText(userField);
        solo.enterText(userField, IntegrationTestHelper.USERNAME);
        solo.enterText((EditText) solo.getView(R.id.txt_password), "wrong-password");

        solo.clickOnDone();
        solo.assertText(R.string.authentication_login_error_password_message);
        solo.clickOnOK();
    }

    @FlakyTest
    public void testLoginAndLogout() throws Exception {
        testLogin();
        solo.logoutViaSettings();
        solo.assertText(R.string.tour_title_1);
    }

    @FlakyTest
    public void testRecoverPassword() throws Throwable {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.clickOnView(R.id.txt_i_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.enterText(0, "some-email-"+System.currentTimeMillis()+"@baz"+System.currentTimeMillis()+".com");
        solo.clickOnOK();

        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
    }

    public void testRecoverPasswordNoInput() throws Exception {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        solo.clickOnView(R.id.txt_i_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.clickOnOK();
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }
}
