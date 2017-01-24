package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.onboarding.auth.tasks.SignupTask.KEY_BIRTHDAY;
import static com.soundcloud.android.onboarding.auth.tasks.SignupTask.KEY_GENDER;
import static com.soundcloud.android.onboarding.auth.tasks.SignupTask.KEY_PASSWORD;
import static com.soundcloud.android.onboarding.auth.tasks.SignupTask.KEY_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class SignUpOperationsTest extends AndroidUnitTest {

    private final Token token = Token.EMPTY;
    private final ApiUser user = ModelFixtures.create(ApiUser.class);

    @Mock Context context;
    @Mock SoundCloudApplication soundCloudApplication;
    @Mock ApiClient apiClient;
    @Mock OAuth oAuth;

    private Bundle bundle;
    private SignUpOperations operations;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
        when(soundCloudApplication.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        when(oAuth.getClientId()).thenReturn("clientId");
        when(oAuth.getClientSecret()).thenReturn("clientSecret");
        bundle = new Bundle();

        operations = new SignUpOperations(context, apiClient, oAuth);
    }

    @Test
    public void shouldReturnAuthResult() throws Exception {
        setupSignupWithUser(GenderOption.FEMALE.name());

        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.getAuthResponse().me.getUser()).isEqualTo(user);
        assertThat(result.wasSuccess()).isTrue();
    }


    @Test
    public void shouldReturnUserWithGenderSetToNull() throws Exception {
        setupSignupWithUser(null);

        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.getAuthResponse().me.getUser()).isEqualTo(user);
        assertThat(result.wasSuccess()).isTrue();
    }

    @Test
    public void shouldProcessLegacyErrorArrayOfNewResponseBodyDuringSignup() throws Exception {
        setupSignupWithError(ApiRequestException.badRequest(null, null, "email_taken"));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasEmailTaken()).isTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupDomainBlacklistedError() throws Exception {
        setupSignupWithError(ApiRequestException.rateLimited(null, null, "domain_blacklisted"));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldReturnSpamAuthTaskResultOnSignupCaptchaRequiredError() throws Exception {
        setupSignupWithError(ApiRequestException.badRequest(null, null, "spamming"));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasSpam()).isTrue();
    }

    @Test
    public void shouldReturnEmailInvalidAuthTaskResultOnSignupEmailInvalidError() throws Exception {
        setupSignupWithError(ApiRequestException.rateLimited(null, null, "invalid_email"));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasEmailInvalid()).isTrue();
    }


    @Test
    public void shouldReturnGenericErrorAuthTaskResultOnSignupOtherErrorWithLegacyErrors() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null,
                                                                 null,
                                                                 "Sorry we couldn't sign you up with the details you provided.",
                                                                 105));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnUnrecognizedErrorCode() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null,
                                                                 null,
                                                                 "Sorry we couldn't sign you up with the details you provided.",
                                                                 180));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupWithUnreconizedError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "unknown", -1));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupForbidden() throws Exception {
        setupSignupWithError(ApiRequestException.notAllowed(null, null));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupServerError() throws Exception {
        setupSignupWithError(ApiRequestException.serverError(null, null));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupUnexpectedResponseStatus() throws Exception {
        setupSignupWithError(ApiRequestException.unexpectedResponse(null, new ApiResponse(null, 403, "body")));
        AuthTaskResult result = operations.signUp(bundle);
        assertThat(result.wasFailure()).isTrue();
    }

    private void setupSignupWithUser(@Nullable String gender) throws Exception {
        bundle.putString(KEY_USERNAME, "username");
        bundle.putString(KEY_PASSWORD, "username");
        bundle.putString(KEY_GENDER, gender);
        bundle.putSerializable(KEY_BIRTHDAY, BirthdayInfo.buildFrom(25));

        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(AuthResponse.class))).thenReturn(new AuthResponse(token, Me.create(user)));
    }

    private void setupSignupWithError(ApiRequestException exception) throws Exception {
        bundle.putString(KEY_USERNAME, "username");
        bundle.putString(KEY_PASSWORD, "username");
        bundle.putString(KEY_GENDER, GenderOption.NO_PREF.name());
        bundle.putSerializable(KEY_BIRTHDAY, BirthdayInfo.buildFrom(25));

        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(AuthResponse.class))).thenThrow(exception);
    }

}
