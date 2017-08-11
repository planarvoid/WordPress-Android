package com.soundcloud.android.tests.auth.login;


import static com.soundcloud.android.framework.TestUser.GPlusAccount;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.TestUser.noGPlusAccount;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.framework.annotation.GoogleAccountTest;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.screens.auth.TermsOfUseScreen;
import com.soundcloud.android.tests.auth.LoginTest;
import org.junit.Ignore;

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
    public void testSCUserLoginFlow() {
        addMockedResponse(ApiEndpoints.SIGN_IN.path(), "sign-in-success.json");
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
    @GoogleAccountTest
    public void testGPlusLoginFlow() {
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
    @GoogleAccountTest
    public void testNoGooglePlusAccountLogin() {
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
    public void testLoginWithFacebookWebFlow() {
        addMockedResponse(ApiEndpoints.SIGN_IN.path(), 200, "sign-in-facebook.json");

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
    public void testLoginWithWrongCredentials() {
        addMockedResponse(ApiEndpoints.SIGN_IN.path(), 400, "sign-in-wrong-password.json");

        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .failToLoginAs(defaultUser.getEmail(), "wrong-password");

        String message = solo.getString(R.string.authentication_login_error_password_message);
        assertThat(loginErrorScreen, is(visible()));
        assertEquals(loginErrorScreen.errorMessage(), message);

        loginErrorScreen.clickOk();
        assertNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
    }

    /*
     * As a User
     * I want to sign out from the app
     * So that I am sure no one can modify my account
     */
    public void testLoginAndLogout() {
        addMockedResponse(ApiEndpoints.SIGN_IN.path(), "sign-in-success.json");

        loginScreen = homeScreen.clickLogInButton();
        loginScreen.loginDefault(scAccount.getEmail(), scAccount.getPassword());

        assertThat(mainNavHelper.goToMore()
                                .clickLogoutAndConfirm(), is(visible()));
    }

    private void assertTermsOfUseDisplayed(TermsOfUseScreen termsOfUseScreen) {
        assertThat(termsOfUseScreen, is(visible()));
        assertEquals(termsOfUseScreen.getTitle(), solo.getString(R.string.auth_disclaimer_title));
        assertEquals(termsOfUseScreen.getDisclaimer(), solo.getString(R.string.auth_disclaimer_message));
    }
}
