package com.soundcloud.android.auth.login;


import android.webkit.WebView;
import com.soundcloud.android.R;
import com.soundcloud.android.auth.LoginTestCase;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.FacebookWebFlowActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.auth.FBWebViewScreen;
import com.soundcloud.android.screens.auth.RecoverPasswordScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.Waiter;

import static com.soundcloud.android.tests.TestUser.GPlusAccount;
import static com.soundcloud.android.tests.TestUser.noGPlusAccount;
import static com.soundcloud.android.tests.TestUser.scAccount;
import static com.soundcloud.android.tests.TestUser.scTestAccount;

/*
 * As a User
 * I want to log in to SoundCloud application
 * So that I can listen to my favourite tracks
 */
public class LoginFlowTest extends LoginTestCase {
    private RecoverPasswordScreen recoveryScreen;
    private FBWebViewScreen FBWebViewScreen;
    private Waiter waiter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        recoveryScreen  = new RecoverPasswordScreen(solo);
        menuScreen      = new MenuScreen(solo);
        FBWebViewScreen = new FBWebViewScreen(solo);
        waiter          = new Waiter(solo);
    }

    /*
     * As a SoundCloud User
     * I want to sign in with my SC account
     * So that I can listen to my favourite tracks
     */
    public void testSCUserLoginFlow()  {
        signupScreen.clickLogInButton();

        loginScreen.loginAs(scTestAccount.getUsername(), scTestAccount.getPassword());

        assertEquals(scTestAccount.getUsername(), menuScreen.getUserName());
    }

    /*
    * As a GooglePlus User
    * I want to sign in with my G+ credentials
    * So that I don't need to create another SC account
    */
    public void testGPlusLoginFlow()  {

        signupScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();

        //FIXME Assuming that we have more than one g+ account, there should be another test for this
        loginScreen.selectUserFromDialog(GPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();

        assertEquals(GPlusAccount.getUsername(), menuScreen.getUserName());
    }

    /*
    * As a Google account User
    * I want to sign in even if I don't have g+ profile
    */
    public void testNoGooglePlusAccountLogin()  {
        signupScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();
        loginScreen.selectUserFromDialog(noGPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();

        assertEquals(noGPlusAccount.getUsername(), menuScreen.getUserName());
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void testLoginWithFacebookWebFlow() throws Throwable {

        // TODO: Control FB SSO on the device.
        if (FacebookSSOActivity.isSupported(getInstrumentation().getTargetContext())) {
            log("Facebook SSO is available, not testing WebFlow");

        }
        signupScreen.clickLogInButton();
        loginScreen.clickOnFBSignInButton();

        //Then termsOfUse dialog should be shown
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();

        WebView webView = solo.assertActivity(FacebookWebFlowActivity.class).getWebView();
        assertNotNull(webView);
        assertTrue(waiter.waitForWebViewToLoad(webView));

        FBWebViewScreen.typeEmail(scAccount.getEmail());
        FBWebViewScreen.typePassword(scAccount.getPassword());
        FBWebViewScreen.submit();

        assertEquals(scAccount.getUsername(), menuScreen.getUserName());
        assertNotNull("FB request timed out", webView.getUrl());
        assertTrue("got url:" + webView.getUrl(), webView.getUrl().contains("facebook.com"));
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void ignore_testLoginWithFBApplication () {
        //TODO Implement this
        // QUESTION How can we control what's installed on device and what's not.
    }

    /*
     * As a User
     * I want to know if I entered wrong password
     * So that I can correct myself
     */
    public void testLoginWithWrongCredentials() {
        signupScreen.clickLogInButton();
        loginScreen.loginAs(scTestAccount.getUsername(), "wrong-password", false);

        solo.assertText(R.string.authentication_login_error_password_message, "We could not log you in");

        loginScreen.clickOkButton();

        solo.assertActivity(OnboardActivity.class);
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
    }

    /*
     * As a User
     * I want to log out from the app
     * So that I am sure no one can modify my account
     */
    public void testLoginAndLogout()  {
        signupScreen.clickLogInButton();
        loginScreen.loginAs(scAccount.getEmail(), scAccount.getPassword());
        waiter.waitForListContent();
        menuScreen.logout();

        solo.assertActivity(OnboardActivity.class);
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

    }

    /*
    * As a User
    * I want to recover my forgotten password
    * So that I don't need to recreate my account
    */
    public void testRecoverPassword() throws Throwable {
        signupScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail("some-email-" + System.currentTimeMillis() + "@baz" + System.currentTimeMillis() + ".com");
        recoveryScreen.clickOkButton();

        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
    }

    /*
    * As a User
    * I want to be notified if I accidentally tap OK button while recovering my password
    * So that I know what went wrong
    */
    public void testRecoverPasswordNoInput()  {
        signupScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        loginScreen.clickOkButton();

        solo.assertText(R.string.authentication_error_incomplete_fields, "Error message should be shown");
    }
}
