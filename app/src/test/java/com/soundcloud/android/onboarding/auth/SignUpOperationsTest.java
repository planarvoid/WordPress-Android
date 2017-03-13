package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.onboarding.auth.SignUpOperations.KEY_BIRTHDAY;
import static com.soundcloud.android.onboarding.auth.SignUpOperations.KEY_GENDER;
import static com.soundcloud.android.onboarding.auth.SignUpOperations.KEY_PASSWORD;
import static com.soundcloud.android.onboarding.auth.SignUpOperations.KEY_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.IOException;

public class SignUpOperationsTest extends AndroidUnitTest {

    @Mock Context context;
    @Mock SoundCloudApplication soundCloudApplication;
    @Mock ApiClient apiClient;
    @Mock OAuth oAuth;
    @Mock JsonTransformer jsonTransformer;
    @Mock ConfigurationOperations configurationOperations;
    @Mock AuthResultMapper authResultMapper;

    private final Token token = Token.EMPTY;
    private final ApiUser user = ModelFixtures.apiUser();
    private final Configuration configuration = ModelFixtures.configuration();
    private Bundle bundle;
    private SignUpOperations operations;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
        when(soundCloudApplication.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        when(oAuth.getClientId()).thenReturn("clientId");
        when(oAuth.getClientSecret()).thenReturn("clientSecret");
        bundle = new Bundle();

        operations = new SignUpOperations(context, apiClient, jsonTransformer, authResultMapper, oAuth, configurationOperations);
    }

    @Test
    public void shouldReturnAuthResult() throws Exception {
        setupSignupWithUser(GenderOption.FEMALE.name());

        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.getAuthResponse().me.getUser()).isEqualTo(user);
        assertThat(result.wasSuccess()).isTrue();
        verify(configurationOperations).saveConfiguration(configuration);
    }

    @Test
    public void shouldReturnUserWithGenderSetToNull() throws Exception {
        setupSignupWithUser(null);

        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.getAuthResponse().me.getUser()).isEqualTo(user);
        assertThat(result.wasSuccess()).isTrue();
        verify(configurationOperations).saveConfiguration(configuration);
    }

    @Test
    public void shouldFailWithTokenRetrievalException() throws Exception {
        setupSignupWithError(TokenRetrievalException.class);

        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.wasSuccess()).isFalse();
        verifyZeroInteractions(configurationOperations);
    }

    @Test
    public void shouldFailWithApiMapperException() throws Exception {
        setupSignupWithError(ApiMapperException.class);

        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.wasSuccess()).isFalse();
        verifyZeroInteractions(configurationOperations);
    }

    @Test
    public void shouldFailWithIoException() throws Exception {
        setupSignupWithError(IOException.class);

        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.wasSuccess()).isFalse();
        verifyZeroInteractions(configurationOperations);
    }

    @Test
    public void shouldFailWithSignupError() throws Exception {
        setupSignupWithUser(null);
        ApiResponse unsuccessfulResponse = setupUnsuccessfulApiResponse();
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(unsuccessfulResponse);
        when(authResultMapper.handleErrorResponse(unsuccessfulResponse)).thenReturn(AuthTaskResult.failure("Error"));


        AuthTaskResult result = operations.signUp(bundle);

        assertThat(result.wasSuccess()).isFalse();
        verifyZeroInteractions(configurationOperations);
        verify(authResultMapper, times(1)).handleErrorResponse(unsuccessfulResponse);
    }

    private void setupSignupWithUser(@Nullable String gender) throws Exception {
        bundle.putString(KEY_USERNAME, "username");
        bundle.putString(KEY_PASSWORD, "username");
        bundle.putString(KEY_GENDER, gender);
        bundle.putSerializable(KEY_BIRTHDAY, BirthdayInfo.buildFrom(25));

        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(new ApiResponse(setupApiRequest(), 200, ""));
        when(jsonTransformer.fromJson(anyString(), any())).thenReturn(new AuthResponse(token, Me.create(user, configuration)));
    }

    private ApiResponse setupUnsuccessfulApiResponse() {
        return new ApiResponse(setupApiRequest(), 422, "");
    }

    private ApiRequest setupApiRequest() {
        return ApiRequest.get("test").forPrivateApi().build();
    }

    private void setupSignupWithError(Class<? extends Throwable> throwable) throws Exception {
        bundle.putString(KEY_USERNAME, "username");
        bundle.putString(KEY_PASSWORD, "username");
        bundle.putString(KEY_GENDER, GenderOption.NO_PREF.name());
        bundle.putSerializable(KEY_BIRTHDAY, BirthdayInfo.buildFrom(25));

        when(apiClient.fetchResponse(any(ApiRequest.class))).thenThrow(throwable);
    }

}
