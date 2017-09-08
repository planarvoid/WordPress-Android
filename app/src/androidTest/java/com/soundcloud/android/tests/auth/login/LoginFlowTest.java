package com.soundcloud.android.tests.auth.login;


import static com.soundcloud.android.R.string.auth_disclaimer_message;
import static com.soundcloud.android.R.string.auth_disclaimer_title;
import static com.soundcloud.android.R.string.authentication_login_error_password_message;
import static com.soundcloud.android.api.ApiEndpoints.SIGN_IN;
import static com.soundcloud.android.framework.AccountAssistant.getAccount;
import static com.soundcloud.android.framework.TestUser.GPlusAccount;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.TestUser.noGPlusAccount;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.screens.auth.TermsOfUseScreen;
import com.soundcloud.android.tests.auth.LoginTest;
import org.junit.Ignore;
import org.junit.Test;

/*
 * As a User
 * I want to sign in to SoundCloud application
 * So that I can listen to my favourite tracks
 */
public class LoginFlowTest extends LoginTest {
    private HomeScreen homeScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }

    /*
     * As a SoundCloud User
     * I want to sign in with the email registered to my SC account
     * So that I can listen to my favourite tracks
     */
    @Test
    public void testSCUserLoginFlow() throws Exception {
        addMockedResponse(SIGN_IN.path(), "sign-in-success.json");
        final StreamScreen streamScreen = homeScreen
                .clickLogInButton()
                .loginDefault(defaultUser.getEmail(), defaultUser.getPassword());

        assertThat(streamScreen, visible());
    }

    /*
    * As a GooglePlus User
    * I want to sign in with my G+ credentials
    * So that I don't need to create another SC account
    */
    @Ignore
    @Test
    public void testGPlusLoginFlow() throws Exception {
        //FIXME Assuming that we have more than one g+ account, there should be another test for this
        final TermsOfUseScreen termsOfUseScreen = homeScreen
                .clickLogInButton()
                .clickSignInWithGoogleButton()
                .selectUserFromDialog(GPlusAccount.getEmail());

        assertTermsOfUseDisplayed(termsOfUseScreen);

        final StreamScreen streamScreen = termsOfUseScreen.clickContinueWithGP();
        assertThat(streamScreen, visible());
    }


    /*
    * As a Google account User
    * I want to sign in even if I don't have g+ profile
    */
    @Ignore
    @Test
    public void testNoGooglePlusAccountLogin() throws Exception {
        final TermsOfUseScreen termsOfUseScreen = homeScreen
                .clickLogInButton()
                .clickSignInWithGoogleButton()
                .selectUserFromDialog(noGPlusAccount.getEmail());

        assertTermsOfUseDisplayed(termsOfUseScreen);

        final StreamScreen streamScreen = termsOfUseScreen.clickContinueWithGP();
        assertThat(streamScreen, visible());
    }

    /*
    * As a Facebook User that has not installed FB application on the phone
    * I want to sign in with my FB credentials
    * So that I don't need to create another account
    */
    @Test
    public void testLoginWithFacebookWebFlow() throws Exception {
        addMockedResponse(SIGN_IN.path(), 200, "sign-in-facebook.json");

        final TermsOfUseScreen termsOfUseScreen = homeScreen
                .clickLogInButton()
                .clickOnFBSignInButton();

        assertTermsOfUseDisplayed(termsOfUseScreen);

        final StreamScreen streamScreen = termsOfUseScreen
                .clickContinueWithFacebookWB()
                .typePassword(scAccount.getPassword())
                .typeEmail(scAccount.getEmail())
                .submit();

        assertThat(streamScreen, visible());
    }

    /*
     * As a User
     * I want to know if I entered wrong password
     * So that I can correct myself
     */
    @Test
    public void testLoginWithWrongCredentials() throws Exception {
        addMockedResponse(SIGN_IN.path(), 400, "sign-in-wrong-password.json");

        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .failToLoginAs(defaultUser.getEmail(), "wrong-password");

        String message = solo.getString(authentication_login_error_password_message);
        assertThat(loginErrorScreen, is(visible()));
        assertEquals(loginErrorScreen.errorMessage(), message);

        loginErrorScreen.clickOk();
        assertNull(getAccount(getInstrumentation().getTargetContext()));
    }

    /*
     * As a User
     * I want to sign out from the app
     * So that I am sure no one can modify my account
     */
    @Test
    public void testLoginAndLogout() throws Exception {
        addMockedResponse(SIGN_IN.path(), "sign-in-success.json");

        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginDefault(scAccount.getEmail(), scAccount.getPassword());

        assertThat(mainNavHelper.goToMore()
                                .clickLogoutAndConfirm(), is(visible()));
    }

    private void assertTermsOfUseDisplayed(TermsOfUseScreen termsOfUseScreen) {
        assertThat(termsOfUseScreen, is(visible()));
        assertEquals(termsOfUseScreen.getTitle(), solo.getString(auth_disclaimer_title));
        assertEquals(termsOfUseScreen.getDisclaimer(), solo.getString(auth_disclaimer_message));
    }
}
