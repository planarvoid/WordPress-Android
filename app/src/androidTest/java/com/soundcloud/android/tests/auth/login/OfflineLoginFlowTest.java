package com.soundcloud.android.tests.auth.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static com.soundcloud.android.R.string.authentication_error_no_connection_message;
import static com.soundcloud.android.R.string.authentication_recover_password_failure;
import static com.soundcloud.android.api.ApiEndpoints.SIGN_IN;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.SoundCloudTestApplication.fromContext;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.android.gms.auth.GoogleAuthException;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.tests.auth.LoginTest;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.TestGplusRegistrationActivity;
import org.junit.Test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

public class OfflineLoginFlowTest extends LoginTest {

    private HomeScreen homeScreen;
    private GooglePlayServicesWrapper googleApi;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        homeScreen = new HomeScreen(solo);
        googleApi = fromContext(getInstrumentation().getTargetContext()).getPlayServicesWrapper();

        connectionHelper.setNetworkConnected(false);
        stopWiremock(); // these depend on no connection
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(get(urlPathMatching(SIGN_IN.path()))
                        .willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));
    }

    @Test
    public void testLoginWithEmailWithoutNetworkConnection() throws Exception {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .failToLoginAs(scAccount.getEmail(), scAccount.getPassword());

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(),
                   is(solo.getString(authentication_error_no_connection_message)));
    }

    @org.junit.Ignore
    @Ignore // TestGplusRegistrationActivity only defined in debug, this breaks preRelease tests
    @Test
    public void testLoginWithGooglePlusAccountWithoutNetworkConnection() throws IOException, GoogleAuthException {
        final Context context = getInstrumentation().getTargetContext();
        when(googleApi.getAccountPickerIntent()).thenReturn(new Intent(context, TestGplusRegistrationActivity.class).setAction("com.google.android.gms.common.account.CHOOSE_ACCOUNT"));
        when(googleApi.getAuthToken(any(Context.class), anyString(), anyString(), any(Bundle.class))).thenThrow(new IOException("network error"));

        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .clickSignInWithGoogleButton()
                .assertTermsOfUseScreen()
                .failToLogin();

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(),
                   is(solo.getString(authentication_error_no_connection_message)));
    }

    @Test
    public void testRecoverPasswordWithoutNetworkConnection() throws Exception {
        homeScreen
                .clickLogInButton()
                .clickForgotPassword()
                .typeEmail(scAccount.getEmail())
                .clickOkButton();

        String message = solo.getString(authentication_recover_password_failure);
        assertTrue(waiter.expectToastWithText(toastObserver, message));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
