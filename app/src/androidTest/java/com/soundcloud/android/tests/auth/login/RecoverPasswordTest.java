package com.soundcloud.android.tests.auth.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.TestUser.generateEmail;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.RecoverPasswordScreen;
import com.soundcloud.android.tests.auth.LoginTest;

public class RecoverPasswordTest extends LoginTest {

    private HomeScreen homeScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }

    public void testResetPasswordSuccessful() throws Exception {
        stubFor(post(urlPathEqualTo(ApiEndpoints.RESET_PASSWORD.path()))
                        .willReturn(aResponse().withStatus(202)));

        loginScreen = homeScreen.clickLogInButton();
        RecoverPasswordScreen recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail(generateEmail());
        recoveryScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_recover_password_success);
        assertTrue(solo.waitForText(message));
    }

    public void testRecoverPasswordFailsForUnknownEmail() throws Exception {
        stubFor(post(urlPathEqualTo(ApiEndpoints.RESET_PASSWORD.path()))
                        .willReturn(aResponse().withStatus(422).withBody("{\"error_key\": \"identifier_not_found\"}")));

        loginScreen = homeScreen.clickLogInButton();
        RecoverPasswordScreen recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail(generateEmail());
        recoveryScreen.clickOkButton();

        String message = solo.getString(R.string.authentication_recover_password_failure_reason,
                                        "Unknown Email Address");
        assertTrue(solo.waitForText(message));
    }

    public void testRecoverPasswordNoInput() {
        homeScreen
                .clickLogInButton()
                .clickForgotPassword()
                .clickOkButton();

        String message = solo.getString(R.string.authentication_error_incomplete_fields);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
