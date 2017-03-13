package com.soundcloud.android.onboarding.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskException;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

public class SignInOperationsTest extends AndroidUnitTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";
    private final ApiUser user = ModelFixtures.create(ApiUser.class);
    private final Configuration configuration = ModelFixtures.create(Configuration.class);
    private final Me me = Me.create(user, configuration);
    @Mock SoundCloudApplication application;
    @Mock Context context;
    @Mock ApiClient apiClient;
    @Mock OAuth oAuth;
    @Mock ConfigurationOperations configurationOperations;
    @Mock EventBus eventBus;
    @Mock AccountOperations accountOperations;
    @Mock Token token;
    @Mock JsonTransformer jsonTransformer;
    @Captor ArgumentCaptor<ApiRequest> apiRequestCaptor;
    private AuthResultMapper authResultMapper = new AuthResultMapper(jsonTransformer);
    private Bundle bundle;
    private SignInOperations operations;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(application);
        when(oAuth.getClientId()).thenReturn("clientId");
        when(oAuth.getClientSecret()).thenReturn("clientSecret");

        bundle = new Bundle();
        operations = new SignInOperations(context, apiClient, authResultMapper, oAuth, configurationOperations, eventBus, accountOperations);
    }

    @Test
    public void createFacebookBundle() throws Exception {
        String facebookToken = "facebookToken";
        Bundle tokenBundle = SignInOperations.getFacebookTokenBundle(facebookToken);
        assertThat(tokenBundle.containsKey(SignInOperations.FACEBOOK_TOKEN_EXTRA)).isTrue();
        assertThat(tokenBundle.getString(SignInOperations.FACEBOOK_TOKEN_EXTRA)).isEqualTo(facebookToken);
    }

    @Test
    public void createGoogleBundle() throws Exception {
        String googleToken = "googletoken";
        Bundle tokenBundle = SignInOperations.getGoogleTokenBundle(googleToken);
        assertThat(tokenBundle.containsKey(SignInOperations.GOOGLE_TOKEN_EXTRA)).isTrue();
        assertThat(tokenBundle.getString(SignInOperations.GOOGLE_TOKEN_EXTRA)).isEqualTo(googleToken);
    }

    @Test
    public void shouldUpdateTokenOnAccountOperations() throws Exception {
        setupDefaultMocksForSuccessfulLogin();

        operations.signIn(bundle);
        verify(accountOperations).updateToken(token);
    }

    @Test
    public void shouldRequestTokenBasedOnBundleContents() throws Exception {
        bundle.putString(SignInOperations.USERNAME_EXTRA, "user");
        bundle.putString(SignInOperations.PASSWORD_EXTRA, "pass");
        operations.signIn(bundle);
        verify(apiClient).fetchMappedResponse(any(ApiRequest.class), eq(AuthResponse.class));
    }

    @Test
    public void shouldReturnAuthenticationFailureIfUserPersistenceFails() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(AuthResponse.class))).thenThrow(new IOException());
        bundle.putString(SignInOperations.USERNAME_EXTRA, "user");
        bundle.putString(SignInOperations.PASSWORD_EXTRA, "pass");

        AuthTaskResult result = operations.signIn(bundle);
        assertThat(result.wasSuccess()).isFalse();
    }

    @Test
    public void shouldSpecifyOriginOfTokenIfTokenIsFromAPI() throws Exception {
        setupDefaultMocksForSuccessfulLogin();

        when(token.getSignup()).thenReturn("api");
        operations.signIn(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.API);
    }

    @Test
    public void shouldNotSpecifySignupOriginIfTokenHasNoOriginString() throws Exception {
        setupDefaultMocksForSuccessfulLogin();

        when(token.getSignup()).thenReturn(null);
        operations.signIn(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }

    @Test
    public void shouldReturnFailureResultIfAddingAccountFails() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(application.addUserAccountAndEnableSync(eq(user), eq(token), any(SignupVia.class))).thenReturn(false);

        assertThat(operations.signIn(bundle).wasSuccess()).isFalse();
    }

    @Test
    public void shouldReturnSuccessResultIfAddingAccountSucceeds() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(application.addUserAccountAndEnableSync(eq(user), eq(token), any(SignupVia.class))).thenReturn(true);

        assertThat(operations.signIn(bundle).wasSuccess()).isTrue();
    }

    @Test
    public void shouldSendConfigurationUpdateRequest() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(application.addUserAccountAndEnableSync(eq(user), eq(token), any(SignupVia.class))).thenReturn(true);

        operations.signIn(bundle);

        verify(configurationOperations).registerDevice(token);
    }

    @Test
    public void unauthorizedRecoverableDeviceBlockResultsInDeviceConflictFailure() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, true));

        AuthTaskResult result = operations.signIn(bundle);

        assertThat(result.wasDeviceConflict()).isTrue();
        assertThat(result.getLoginBundle()).isSameAs(bundle);
        assertThat(result.getLoginBundle().getBoolean(SignInOperations.IS_CONFLICTING_DEVICE)).isTrue();
    }

    @Test
    public void unauthorizedUnrecoverableDeviceBlockResultsInDeviceBlockFailure() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, false));

        AuthTaskResult result = operations.signIn(bundle);

        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasDeviceBlock()).isTrue();
    }

    @Test
    public void failureToForceRegisterDeviceReturnsGenericError() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(configurationOperations.forceRegisterDevice(token)).thenReturn(new DeviceManagement(false, true));
        bundle.putBoolean(SignInOperations.IS_CONFLICTING_DEVICE, true);

        AuthTaskResult result = operations.signIn(bundle);

        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.getException()).isInstanceOf(AuthTaskException.class);
    }

    @Test
    public void successfulForceRegisterDeviceContinuesLogin() throws Exception {
        setupDefaultMocksForSuccessfulLogin();
        when(configurationOperations.forceRegisterDevice(token)).thenReturn(new DeviceManagement(true, false));
        bundle.putBoolean(SignInOperations.IS_CONFLICTING_DEVICE, true);
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);

        assertThat(operations.signIn(bundle).wasSuccess()).isTrue();
    }

    @Test
    public void requestBodyWithInvalidBundle() throws Exception {
        AuthTaskResult authTaskResult = operations.signIn(bundle);

        assertThat(authTaskResult.wasSuccess()).isFalse();
    }

    private void setupDefaultMocksForSuccessfulLogin() throws IOException, ApiRequestException, ApiMapperException {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(AuthResponse.class))).thenReturn(new AuthResponse(token, me));
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(true, false));
        bundle.putString(SignInOperations.USERNAME_EXTRA, USERNAME);
        bundle.putString(SignInOperations.PASSWORD_EXTRA, PASSWORD);
    }
}
