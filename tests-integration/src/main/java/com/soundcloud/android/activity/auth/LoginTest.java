package com.soundcloud.android.activity.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.suitebuilder.annotation.Suppress;
import android.webkit.WebView;
import android.widget.EditText;

public class LoginTest extends ActivityTestCase<Main> {
    public LoginTest() {
        super(Main.class);
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

        EditText userField = (EditText) solo.getView(R.id.txt_email_address);
        solo.clearEditText(userField);
        solo.enterText(userField, IntegrationTestHelper.USERNAME);
        solo.enterText((EditText) solo.getView(R.id.txt_password), "wrong-password");

        solo.clickOnButton(solo.getString(R.string.btn_login));
        solo.assertText(R.string.authentication_login_error_password_message);
        solo.clickOnOK();
    }

    public void testLoginAndLogout() throws Exception {
        testLogin();
        solo.logoutViaSettings();
        solo.assertActivity(Start.class);
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
