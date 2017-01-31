package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignUpOperations;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

public class SignupTaskTest extends AndroidUnitTest {

    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private ApiClient apiClient;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private SoundCloudApplication application;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private FeatureFlags featureFlags;
    @Mock private SignUpOperations signUpOperations;

    private SignupTask signupTask;

    @Before
    public void setUp() throws Exception {
        signupTask = new SignupTask(application,
                                    storeUsersCommand,
                                    tokenInformationGenerator,
                                    apiClient,
                                    syncInitiatorBridge,
                                    featureFlags,
                                    signUpOperations);

        when(featureFlags.isEnabled(Flag.AUTH_API_MOBILE)).thenReturn(false);
    }

    @Test
    public void shouldReturnUser() throws Exception {
        PublicApiUser user = new PublicApiUser(123L);
        setupSignupWithUser(user);

        LegacyAuthTaskResult result = doSignup();
        assertThat(result.getUser()).isEqualTo(user.toApiMobileUser());
        assertThat(result.wasSuccess()).isTrue();
    }

    @Test
    public void shouldReturnUserWithGenderSetToNull() throws Exception {
        PublicApiUser user = new PublicApiUser(123L);
        setupSignupWithUser(user);

        LegacyAuthTaskResult result = doSignupWithNullGender();
        assertThat(result.getUser()).isEqualTo(user.toApiMobileUser());
        assertThat(result.wasSuccess()).isTrue();
    }

    @Test
    public void shouldProcessLegacyErrorArrayOfNewResponseBodyDuringSignup() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email has already been taken", 101));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasEmailTaken()).isTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupDomainBlacklistedError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email domain is blacklisted.", 102));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldReturnSpamAuthTaskResultOnSignupCaptchaRequiredError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null,
                                                                 null,
                                                                 "Spam detected, login on web page with captcha.",
                                                                 103));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasSpam()).isTrue();
    }

    @Test
    public void shouldReturnEmailInvalidAuthTaskResultOnSignupEmailInvalidError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "Email is invalid.", 104));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasEmailInvalid()).isTrue();
    }

    @Test
    public void shouldReturnGenericErrorAuthTaskResultOnSignupOtherErrorWithLegacyErrors() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null,
                                                                 null,
                                                                 "Sorry we couldn't sign you up with the details you provided.",
                                                                 105));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnUnrecognizedErrorCode() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null,
                                                                 null,
                                                                 "Sorry we couldn't sign you up with the details you provided.",
                                                                 180));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupWithUnreconizedError() throws Exception {
        setupSignupWithError(ApiRequestException.validationError(null, null, "unknown", -1));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupForbidden() throws Exception {
        setupSignupWithError(ApiRequestException.notAllowed(null, null));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupServerError() throws Exception {
        setupSignupWithError(ApiRequestException.serverError(null, null));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupUnexpectedResponseStatus() throws Exception {
        setupSignupWithError(ApiRequestException.unexpectedResponse(null, new ApiResponse(null, 403, "body")));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldReturnFailureWhenFailsVerifyingScope() throws Exception {
        when(tokenInformationGenerator.verifyScopes(Token.SCOPE_SIGNUP))
                .thenThrow(new TokenRetrievalException(new Exception()));
        LegacyAuthTaskResult result = doSignup();
        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void forwardToOperationsWhenFeatureFlagEnabled() throws Exception {
        Token token = Token.EMPTY;
        Me me = Me.create(ModelFixtures.create(ApiUser.class));
        Bundle bundle = getParamsBundle();

        when(featureFlags.isEnabled(Flag.AUTH_API_MOBILE)).thenReturn(true);
        when(signUpOperations.signUp(bundle)).thenReturn(AuthTaskResult.success(new AuthResponse(token, me), SignupVia.API));

        signupTask.doInBackground(bundle);
        verify(signUpOperations).signUp(bundle);
    }

    private LegacyAuthTaskResult doSignup() {
        return signupTask.doSignup(getParamsBundle());
    }

    private LegacyAuthTaskResult doSignupWithNullGender() {
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

        when(apiClient.fetchMappedResponse(argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_USERS)),
                                           eq(PublicApiUser.class)))
                .thenReturn(user);
    }

    private void setupSignupWithError(ApiRequestException exception) throws Exception {
        when(tokenInformationGenerator.verifyScopes(Token.SCOPE_SIGNUP)).thenReturn(new Token("access", "refresh"));
        when(apiClient.fetchMappedResponse(argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_USERS)),
                                           eq(PublicApiUser.class)))
                .thenThrow(exception);
    }

}
