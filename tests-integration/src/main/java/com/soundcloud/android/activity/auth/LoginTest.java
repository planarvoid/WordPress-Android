package com.soundcloud.android.activity.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.FlakyTest;
import android.webkit.WebView;
import android.widget.EditText;


public class LoginTest extends ActivityTestCase<News> {
    public LoginTest() {
        super(News.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.setUp();
    }

    public void testLogin() throws Exception {
        // TODO: make first in suite sleep
        solo.sleep(1000);

        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertText(R.string.authentication_log_in);

        EditText userField = (EditText) solo.getView(R.id.txt_email_address);

        solo.clearEditText(userField);
        solo.enterText(userField, IntegrationTestHelper.USERNAME);
        solo.enterText((EditText) solo.getView(R.id.txt_password), IntegrationTestHelper.PASSWORD);

        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertDialogClosed();
        solo.assertText(R.string.side_menu_stream);
    }

    @FlakyTest
    public void testLoginWithFacebookWebFlow() throws Exception {
        if (FacebookSSO.isSupported(getInstrumentation().getTargetContext())) {
            log("Facebook SSO is available, not testing WebFlow");
            return;
        }
        solo.clickOnText("Facebook");
        solo.assertDialogClosed();
        WebView webView = solo.assertActivity(FacebookWebFlow.class).getWebView();
        assertNotNull(webView);
        int i = 0;
        while (webView.getUrl() == null && i++ < 40) {
            solo.sleep(500);
        }
        assertNotNull("FB request timed out", webView.getUrl());
        assertTrue("got url:" + webView.getUrl(), webView.getUrl().startsWith("http://m.facebook.com/login.php"));
    }

    public void testLoginWithWrongCredentials() {
        solo.clickOnButtonResId(R.string.btn_login);
        solo.assertText(R.string.authentication_log_in);

        EditText userField = (EditText) solo.getView(R.id.txt_email_address);
        solo.clearEditText(userField);
        solo.enterText(userField, IntegrationTestHelper.USERNAME);
        solo.enterText((EditText) solo.getView(R.id.txt_password), "wrong-password");

        solo.clickOnButton(solo.getString(R.string.btn_login));
        solo.assertText(R.string.authentication_login_error_password_message);
        solo.clickOnOK();
    }

    @FlakyTest
    public void testLoginAndLogout() throws Exception {
        testLogin();

        solo.logoutViaSettings();
        solo.assertActivity(Start.class);
    }

    @FlakyTest
    public void testRecoverPassword() throws Exception {
        solo.clickOnText(R.string.authentication_log_in);
        solo.clickOnText(R.string.authentication_I_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.enterText(0, "some-email-"+System.currentTimeMillis()+"@baz"+System.currentTimeMillis()+".com");
        solo.clickOnOK();

        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
    }

    public void testRecoverPasswordNoInput() throws Exception {
        solo.clickOnText(R.string.authentication_log_in);
        solo.clickOnText(R.string.authentication_I_forgot_my_password);
        solo.assertActivity(Recover.class);

        solo.clickOnOK();
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }
}
