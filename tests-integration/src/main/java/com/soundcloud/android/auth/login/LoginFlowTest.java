package com.soundcloud.android.auth.login;


import static com.soundcloud.android.tests.TestUser.GPlusAccount;
import static com.soundcloud.android.tests.TestUser.noGPlusAccount;
import static com.soundcloud.android.tests.TestUser.scAccount;
import static com.soundcloud.android.tests.TestUser.scTestAccount;

import com.soundcloud.android.R;
import com.soundcloud.android.auth.LoginTestCase;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.auth.FBWebViewScreen;
import com.soundcloud.android.screens.auth.RecoverPasswordScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.Waiter;

/*
 * As a User
 * I want to log in to SoundCloud application
 * So that I can listen to my favourite tracks
 */
public class LoginFlowTest extends LoginTestCase {
    private RecoverPasswordScreen recoveryScreen;
    private FBWebViewScreen FBWebViewScreen;
    private HomeScreen homeScreen;
    private Waiter waiter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);

        menuScreen      = new MenuScreen(solo);
        waiter          = new Waiter(solo);
    }

    /*
     * As a SoundCloud User
     * I want to sign in with my SC account
     * So that I can listen to my favourite tracks
     */
    public void testSCUserLoginFlow()  {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginAs(scTestAccount.getPermalink(), scTestAccount.getPassword());
        //TODO: MainScreen.menu().getUsername();
        menuScreen.open();
        assertEquals(scTestAccount.getPermalink(), menuScreen.getUserName());
    }

    /*
    * As a GooglePlus User
    * I want to sign in with my G+ credentials
    * So that I don't need to create another SC account
    */
    public void testGPlusLoginFlow()  {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();

        //FIXME Assuming that we have more than one g+ account, there should be another test for this
        loginScreen.selectUserFromDialog(GPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
//        TODO: DialogElement
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();
        menuScreen.open();
        assertEquals(GPlusAccount.getPermalink(), menuScreen.getUserName());
    }

    /*
    * As a Google account User
    * I want to sign in even if I don't have g+ profile
    */
    public void testNoGooglePlusAccountLogin()  {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();
        loginScreen.selectUserFromDialog(noGPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
        //TODO: DIALOG
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();

        menuScreen.open();
        assertEquals(noGPlusAccount.getPermalink(), menuScreen.getUserName());
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void testLoginWithFacebookWebFlow() {

        // TODO: Control FB SSO on the device.
        if (FacebookSSOActivity.isSupported(getInstrumentation().getTargetContext())) {
            log("Facebook SSO is available, not testing WebFlow");

        }
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickOnFBSignInButton();

        //Then termsOfUse dialog should be shown
        //TODO: DialogElement
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();

        FBWebViewScreen = new FBWebViewScreen(solo);

        FBWebViewScreen.typePassword(scAccount.getPassword());
        FBWebViewScreen.typeEmail(scAccount.getEmail());
        FBWebViewScreen.submit();
        menuScreen.open();

        assertEquals(scAccount.getPermalink(), menuScreen.getUserName());
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
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginAs(scTestAccount.getPermalink(), "wrong-password", false);
        //TODO: DialogElement
        solo.assertText(R.string.authentication_login_error_password_message, "We could not log you in");
        loginScreen.clickOkButton();
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
    }

    /*
     * As a User
     * I want to log out from the app
     * So that I am sure no one can modify my account
     */
    public void testLoginAndLogout()  {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginAs(scAccount.getEmail(), scAccount.getPassword());
        menuScreen.logout();

        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));

    }

    /*
    * As a User
    * I want to recover my forgotten password
    * So that I don't need to recreate my account
    */
    public void testRecoverPassword() throws Throwable {
        loginScreen = homeScreen.clickLogInButton();
        recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail("some-email-" + System.currentTimeMillis() + "@baz" + System.currentTimeMillis() + ".com");
        recoveryScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
        waiter.expectToast().toHaveText(message);
    }

    /*
    * As a User
    * I want to be notified if I accidentally tap OK button while recovering my password
    * So that I know what went wrong
    */
    public void testRecoverPasswordNoInput()  {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        loginScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_error_incomplete_fields);
        waiter.expectToast().toHaveText(message);
    }
}
