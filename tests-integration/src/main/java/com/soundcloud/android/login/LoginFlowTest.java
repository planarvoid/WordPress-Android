package com.soundcloud.android.login;


import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.auth.FacebookWebFlow;
import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.model.User;
import com.soundcloud.android.screens.RecoverPasswordScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.UsersHelper;
import com.soundcloud.android.screens.FBWebViewScreen;
import com.soundcloud.android.screens.LoginScreen;
import com.soundcloud.android.screens.MenuScreen;

import android.webkit.WebView;

/*
 * As a User
 * I want to log in to SoundCloud application
 * So that I can listen to my favourite tracks
 */
public class LoginFlowTest extends ActivityTestCase<Onboard> {
    private LoginScreen loginScreen;
    private RecoverPasswordScreen recoveryScreen;
    private FBWebViewScreen FBWebViewScreen;

    public LoginFlowTest() {
        super(Onboard.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.setUp();

        loginScreen = new LoginScreen(solo);
        recoveryScreen = new RecoverPasswordScreen(solo);
        menuScreen = new MenuScreen(solo);
        FBWebViewScreen = new FBWebViewScreen(solo);
    }

    /*
     * As a SoundCloud User
     * I want to sign in with my SC account
     * So that I can listen to my favourite tracks
     */
    public void testSCUserLoginFlow() throws Exception {
        UsersHelper scUser = new UsersHelper("scTestAccount");

        loginScreen.clickLogInButton();
        loginScreen.loginAs(scUser.login(), scUser.password());

        assertEquals(scUser.login(), menuScreen.getUserName());
    }

    /*
    * As a GooglePlus User
    * I want to sign in with my G+ credentials
    * So that I don't need to create another SC account
    */
    public void testGPlusLoginFlow() throws Exception {
        UsersHelper user = new UsersHelper("GPlusAccount");

        loginScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();

        //FIXME Assuming that we have more than one g+ account, there should be another test for this
        loginScreen.selectUserFromDialog(user.email());

        solo.assertText("One more step");

        loginScreen.clickOnContinueButton();
        //TODO Finish the flow once we fix the g+ sign in
        //assertEquals(user.login(), menuScreen.getUserName());
    }

    /*
    * As a Google account User
    * I want to be informed that my account doesn't meet SC needs
    * So that I can set up proper google Plus account
    */
    public void testGoogleAccountLoginError() throws Exception {
        UsersHelper user = new UsersHelper("noGPlusAccount");

        loginScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();
        loginScreen.selectUserFromDialog(user.email());

        solo.assertText("One more step");

        loginScreen.clickOnContinueButton();

        solo.assertText(R.string.error_google_sign_in_failed, "Error message should be shown");
        assertNull(getLoggedInUser().username);
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void testLoginWithFacebookWebFlow() throws Throwable {
        UsersHelper user = new UsersHelper("scAccount");

        if (FacebookSSO.isSupported(getInstrumentation().getTargetContext())) {
            log("Facebook SSO is available, not testing WebFlow");

        }
        loginScreen.clickLogInButton();
        solo.performClick(this, R.id.facebook_btn);
        loginScreen.clickOnContinueButton();
        solo.assertDialogClosed();
        WebView webView = solo.assertActivity(FacebookWebFlow.class).getWebView();
        assertNotNull(webView);
        int i = 0;
        while (webView.getUrl() == null && i++ < 40) {
            solo.sleep(500);
        }

        FBWebViewScreen.typeEmail(user.email());
        FBWebViewScreen.typePassword(user.password());
        FBWebViewScreen.submit();

        assertEquals(user.login(), menuScreen.getUserName());
        assertNotNull("FB request timed out", webView.getUrl());
        assertTrue("got url:" + webView.getUrl(), webView.getUrl().contains("facebook.com"));
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void testLoginWithFBApplication () {
        //TODO Implement this
        // QUESTION How can we control what's installed on device and what's not.
    }

    /*
     * As a User
     * I want to know if I entered wrong password
     * So that I can correct myself
     */
    public void testLoginWithWrongCredentials() {
        UsersHelper scUser = new UsersHelper("scTestAccount");

        loginScreen.clickLogInButton();
        loginScreen.loginAs(scUser.login(), "wrong-password", false);

        solo.assertText(R.string.authentication_login_error_password_message, "We could not log you in");

        loginScreen.clickOkButton();

        solo.assertActivity(Onboard.class);
        assertNull(getLoggedInUser().username);
    }

    /*
     * As a User
     * I want to log out from the app
     * So that I am sure no one can modify my account
     */
    public void testLoginAndLogout() throws Exception {
        UsersHelper user = new UsersHelper("scAccount");

        loginScreen.clickLogInButton();
        loginScreen.loginAs(user.email(), user.password());
        menuScreen.logout();

        assertNull(getLoggedInUser().username);
        solo.assertActivity(Onboard.class);
    }

    /*
    * As a User
    * I want to recover my forgotten password
    * So that I don't need to recreate my account
    */
    public void testRecoverPassword() throws Throwable {
        loginScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail("some-email-"+System.currentTimeMillis()+"@baz"+System.currentTimeMillis()+".com");
        recoveryScreen.clickOkButton();

        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
    }

    /*
    * As a User
    * I want to be notified if I accidentally tap OK button while recovering my password
    * So that I know what went wrong
    */
    public void testRecoverPasswordNoInput() throws Exception {
        loginScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        loginScreen.clickOkButton();

        solo.assertText(R.string.authentication_error_incomplete_fields, "Error message should be shown");
    }

    private User getLoggedInUser() {
        return ((SoundCloudApplication)getActivity().getApplication()).getLoggedInUser();
    }
}
