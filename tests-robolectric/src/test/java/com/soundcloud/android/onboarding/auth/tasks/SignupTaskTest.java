package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.LegacyUserStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class SignupTaskTest {
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private ApiClient apiClient;
    @Mock private LegacyUserStorage userStorage;
    @Mock private SoundCloudApplication application;

    private SignupTask signupTask;

    @Before
    public void setUp() throws Exception {
        signupTask = new SignupTask(application, userStorage, tokenInformationGenerator, apiClient);
    }

    @Test
    public void shouldReturnUser() throws Exception {
        PublicApiUser user = new PublicApiUser(123L);
        setupSignupWithUser(user);

        AuthTaskResult result = doSignup();
        expect(result.getUser()).toEqual(user);
        expect(result.wasSuccess()).toBeTrue();
    }

    @Test
    public void shouldReturnUserWithGenderSetToNull() throws Exception {
        PublicApiUser user = new PublicApiUser(123L);
        setupSignupWithUser(user);

        AuthTaskResult result = doSignupWithNullGender();
        expect(result.getUser()).toEqual(user);
        expect(result.wasSuccess()).toBeTrue();
    }

    @Test
    public void shouldProcessLegacyErrorArrayOfNewResponseBodyDuringSignup() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email has already been taken", 101));
        AuthTaskResult result = doSignup();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupDomainBlacklistedError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email domain is blacklisted.", 102));
        AuthTaskResult result = doSignup();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldReturnSpamAuthTaskResultOnSignupCaptchaRequiredError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Spam detected, login on web page with captcha.", 103));
        AuthTaskResult result = doSignup();
        expect(result.wasSpam()).toBeTrue();
    }

    @Test
    public void shouldReturnEmailInvalidAuthTaskResultOnSignupEmailInvalidError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email is invalid.", 104));
        AuthTaskResult result = doSignup();
        expect(result.wasEmailInvalid()).toBeTrue();
    }

    @Test
    public void shouldReturnGenericErrorAuthTaskResultOnSignupOtherErrorWithLegacyErrors() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Sorry we couldn't sign you up with the details you provided.", 105));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnUnrecognizedErrorCode() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Sorry we couldn't sign you up with the details you provided.", 180));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupWithUnreconizedError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "unknown", -1));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupForbidden() throws Exception {
        setupSignupWithError(ApiRequestException.notAllowed(null, null));
        AuthTaskResult result = doSignup();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupServerError() throws Exception {
        setupSignupWithError(ApiRequestException.serverError(null, null));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupUnexpectedResponseStatus() throws Exception {
        setupSignupWithError(ApiRequestException.unexpectedResponse(null, new ApiResponse(null, 403, "body")));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureWhenFailsVerifyingScope() throws Exception {
        when(tokenInformationGenerator.verifyScopes(Token.SCOPE_SIGNUP))
                .thenThrow(new TokenRetrievalException(new Exception()));
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    private AuthTaskResult doSignup() {
        return signupTask.doSignup(getParamsBundle());
    }

    private AuthTaskResult doSignupWithNullGender() {
        return signupTask.doSignup(getParamsBundleWithNullGender());
    }

    private Bundle getParamsBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SignupTask.KEY_USERNAME, "username");
        bundle.putString(SignupTask.KEY_PASSWORD, "password");
        bundle.putSerializable(SignupTask.KEY_BIRTHDAY, BirthdayInfo.buildFrom(22));
        bundle.putString(SignupTask.KEY_GENDER, "fluid");
        return bundle;
    }

    private Bundle getParamsBundleWithNullGender() {
        Bundle bundle = new Bundle();
        bundle.putString(SignupTask.KEY_USERNAME, "username");
        bundle.putString(SignupTask.KEY_PASSWORD, "password");
        bundle.putSerializable(SignupTask.KEY_BIRTHDAY, BirthdayInfo.buildFrom(22));
        bundle.putString(SignupTask.KEY_GENDER, null);
        return bundle;
    }

    private void setupSignupWithUser(PublicApiUser user) throws Exception {
        when(tokenInformationGenerator.verifyScopes(Token.SCOPE_SIGNUP)).thenReturn(new Token("access", "refresh"));

        when(apiClient.fetchMappedResponse(argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_USERS)), eq(PublicApiUser.class)))
                .thenReturn(user);
    }

    private void setupSignupWithError(ApiRequestException exception) throws Exception {
        when(tokenInformationGenerator.verifyScopes(Token.SCOPE_SIGNUP)).thenReturn(new Token("access", "refresh"));
        when(apiClient.fetchMappedResponse(argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_USERS)), eq(PublicApiUser.class)))
                .thenThrow(exception);
    }
}
