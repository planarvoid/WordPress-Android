package com.soundcloud.android.tests.auth.login;

import static com.soundcloud.android.framework.TestUser.GPlusAccount;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.tests.auth.LoginTest;

public class OfflineLoginFlowTest extends LoginTest {
    private HomeScreen homeScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
        networkManager.switchWifiOff();
    }

    public void testLoginWithEmailWithoutNetworkConnection() {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .failToLoginAs(scAccount.getEmail(), scAccount.getPassword());

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(), is(solo.getString(R.string.authentication_error_no_connection_message)));
    }

    public void testLoginWithGooglePlusAccountWithoutNetworkConnection() {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .clickSignInWithGoogleButton()
                .selectUserFromDialog(GPlusAccount.getEmail())
                .failToLogin();

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(), is(solo.getString(R.string.authentication_error_no_connection_message)));
    }

    public void testLoginWithFacebookAccountWithoutNetworkConnection() {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .clickOnFBSignInButton()
                .failToLogin();

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(), is(solo.getString(R.string.facebook_authentication_failed_message)));
    }

    public void testRecoverPasswordWithoutNetworkConnection() {
        homeScreen
                .clickLogInButton()
                .clickForgotPassword()
                .typeEmail(scAccount.getEmail())
                .clickOkButton();

        String message = solo.getString(R.string.authentication_recover_password_failure);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
