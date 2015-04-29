package com.soundcloud.android.tests.auth.login;


import static com.soundcloud.android.framework.TestUser.GPlusAccount;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.TestUser.generateEmail;
import static com.soundcloud.android.framework.TestUser.noGPlusAccount;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.auth.FBWebViewScreen;
import com.soundcloud.android.screens.auth.RecoverPasswordScreen;
import com.soundcloud.android.tests.auth.LoginTest;

/*
 * As a User
 * I want to log in to SoundCloud application
 * So that I can listen to my favourite tracks
 */
public class LoginFlowTest extends LoginTest {
    private RecoverPasswordScreen recoveryScreen;
    private FBWebViewScreen FBWebViewScreen;
    private HomeScreen homeScreen;
    private Waiter waiter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);

        menuScreen = new MenuScreen(solo);
        waiter = new Waiter(solo);
    }

    /*
     * As a SoundCloud User
     * I want to sign in with the email registered to my SC account
     * So that I can listen to my favourite tracks
     */
    public void testSCUserLoginFlow() {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginAs(defaultUser.getEmail(), defaultUser.getPassword());
        assertThat(new StreamScreen(solo), visible());
    }

    /*
    * As a GooglePlus User
    * I want to sign in with my G+ credentials
    * So that I don't need to create another SC account
    */
    public void testGPlusLoginFlow() {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();

        //FIXME Assuming that we have more than one g+ account, there should be another test for this
        loginScreen.selectUserFromDialog(GPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
//        TODO: DialogElement
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();
        assertThat(new StreamScreen(solo), visible());
    }

    /*
    * As a Google account User
    * I want to sign in even if I don't have g+ profile
    */
    public void testNoGooglePlusAccountLogin() {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickSignInWithGoogleButton();
        loginScreen.selectUserFromDialog(noGPlusAccount.getEmail());

        // Then termsOfUse dialog should be shown
        //TODO: DIALOG
        solo.assertText(R.string.auth_disclaimer_title);
        solo.assertText(R.string.auth_disclaimer_message);

        loginScreen.clickOnContinueButton();
        assertThat(new StreamScreen(solo), visible());
    }

    /*
    * As a Facebook User that has not installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void testLoginWithFacebookWebFlow() {
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
        assertThat(new StreamScreen(solo), visible());
    }

    /*
    * As a Facebook User that has installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    public void ignore_testLoginWithFBApplication() {
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
        loginScreen.loginAs(defaultUser.getEmail(), "wrong-password", false);
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
    public void testLoginAndLogout() {
        loginScreen = homeScreen.clickLogInButton();
        StreamScreen streamScreen = loginScreen.loginAs(scAccount.getEmail(), scAccount.getPassword());

        assertThat(streamScreen.actionBar()
                .clickSettingsOverflowButton()
                .clickLogoutAndConfirm(), is(visible()));
    }

    /*
    * As a User
    * I want to recover my forgotten password
    * So that I don't need to recreate my account
    */
    public void testRecoverPassword() throws Throwable {
        loginScreen = homeScreen.clickLogInButton();
        recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail(generateEmail());
        recoveryScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_recover_password_failure_reason, "Unknown Email Address");
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    /*
    * As a User
    * I want to be notified if I accidentally tap OK button while recovering my password
    * So that I know what went wrong
    */
    public void testRecoverPasswordNoInput() {
        loginScreen = homeScreen.clickLogInButton();
        loginScreen.clickForgotPassword();
        loginScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_error_incomplete_fields);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
