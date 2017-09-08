package com.soundcloud.android.tests.auth.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.R.string.authentication_error_incomplete_fields;
import static com.soundcloud.android.R.string.authentication_recover_password_failure_reason;
import static com.soundcloud.android.R.string.authentication_recover_password_success;
import static com.soundcloud.android.api.ApiEndpoints.RESET_PASSWORD;
import static com.soundcloud.android.framework.TestUser.generateEmail;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.RecoverPasswordScreen;
import com.soundcloud.android.tests.auth.LoginTest;
import org.junit.Test;

public class RecoverPasswordTest extends LoginTest {

    private HomeScreen homeScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }

    @Test
    public void testResetPasswordSuccessful() throws Exception {
        stubFor(post(urlPathEqualTo(RESET_PASSWORD.path()))
                        .willReturn(aResponse().withStatus(202)));

        loginScreen = homeScreen.clickLogInButton();
        RecoverPasswordScreen recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail(generateEmail());
        recoveryScreen.clickOkButton();

        String message = solo.getString(authentication_recover_password_success);
        assertTrue(solo.waitForText(message));
    }

    @Test
    public void testRecoverPasswordFailsForUnknownEmail() throws Exception {
        stubFor(post(urlPathEqualTo(RESET_PASSWORD.path()))
                        .willReturn(aResponse().withStatus(422).withBody("{\"error_key\": \"identifier_not_found\"}")));

        loginScreen = homeScreen.clickLogInButton();
        RecoverPasswordScreen recoveryScreen = loginScreen.clickForgotPassword();
        recoveryScreen.typeEmail(generateEmail());
        recoveryScreen.clickOkButton();

        String message = solo.getString(authentication_recover_password_failure_reason,
                                        "Unknown Email Address");
        assertTrue(solo.waitForText(message));
    }

    @Test
    public void testRecoverPasswordNoInput() throws Exception {
        homeScreen
                .clickLogInButton()
                .clickForgotPassword()
                .clickOkButton();

        String message = solo.getString(authentication_error_incomplete_fields);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
