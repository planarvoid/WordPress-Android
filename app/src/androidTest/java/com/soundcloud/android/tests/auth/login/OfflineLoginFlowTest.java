package com.soundcloud.android.tests.auth.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.scAccount;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.android.gms.auth.GoogleAuthException;
import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.android.tests.auth.LoginTest;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.TestGplusRegistrationActivity;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

public class OfflineLoginFlowTest extends LoginTest {

    private LoginManager facebookLoginManager;
    private HomeScreen homeScreen;
    private GooglePlayServicesWrapper googleApi;
    @Captor private ArgumentCaptor<FacebookCallback<LoginResult>> facebookCallbackArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        super.setUp();

        homeScreen = new HomeScreen(solo);
        final SoundCloudTestApplication application = SoundCloudTestApplication.fromContext(getInstrumentation().getTargetContext());
        facebookLoginManager = application.getLoginManager();
        doAnswer(invocationOnMock -> {
            verify(facebookLoginManager).registerCallback(any(CallbackManager.class), facebookCallbackArgumentCaptor.capture());
            facebookCallbackArgumentCaptor.getValue().onError(new FacebookAuthorizationException("net::ERR_INTERNET_DISCONNECTED"));
            return null;
        }).when(facebookLoginManager).logInWithReadPermissions(any(Activity.class), anyCollection());
        googleApi = application.getPlayServicesWrapper();

        connectionHelper.setNetworkConnected(false);
        stopWiremock(); // these depend on no connection
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(get(urlPathMatching(ApiEndpoints.SIGN_IN.path()))
                        .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    }

    public void testLoginWithEmailWithoutNetworkConnection() {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .failToLoginAs(scAccount.getEmail(), scAccount.getPassword());

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(),
                   is(solo.getString(R.string.authentication_error_no_connection_message)));
    }

    @Ignore // TestGplusRegistrationActivity only defined in debug, this breaks preRelease tests
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
                   is(solo.getString(R.string.authentication_error_no_connection_message)));
    }

    public void testLoginWithFacebookAccountWithoutNetworkConnection() {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .clickOnFBSignInButton()
                .failToLoginWithResult();

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(),
                   is(solo.getString(R.string.authentication_error_no_connection_message)));
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
