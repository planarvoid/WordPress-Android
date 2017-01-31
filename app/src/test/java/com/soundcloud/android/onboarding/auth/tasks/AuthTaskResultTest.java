package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Bundle;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class AuthTaskResultTest {

    @Test
    public void shouldCreateSuccessResult() throws CreateModelException {
        Token token = Token.EMPTY;
        ApiUser user = ModelFixtures.create(ApiUser.class);
        SignupVia signupVia = SignupVia.NONE;

        AuthTaskResult result = AuthTaskResult.success(new AuthResponse(token, Me.create(user)), signupVia);

        assertThat(result.wasSuccess()).isTrue();
        assertThat(result.getAuthResponse().me.getUser()).isEqualTo(user);
        assertThat(result.getSignupVia()).isEqualTo(signupVia);
    }

    @Test
    public void shouldCreateFailureResult() {
        String errorMessage = "Error Message Description";

        AuthTaskResult result = AuthTaskResult.failure(errorMessage);

        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldCreateEmailTakenResult() {
        AuthTaskResult result = AuthTaskResult.emailTaken(ApiRequestException.validationError(null,
                                                                                              null,
                                                                                              "email_taken",
                                                                                              999));
        assertThat(result.wasEmailTaken()).isTrue();
    }

    @Test
    public void shouldCreateSpamResult() {
        AuthTaskResult result = AuthTaskResult.spam(ApiRequestException.validationError(null, null, "spam", 999));
        assertThat(result.wasSpam()).isTrue();
    }

    @Test
    public void shouldCreateDeniedResult() {
        AuthTaskResult result = AuthTaskResult.denied(ApiRequestException.validationError(null, null, "denied", 999));
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldCreateEmailInvalidResult() {
        AuthTaskResult result = AuthTaskResult.emailInvalid(ApiRequestException.validationError(null,
                                                                                                null,
                                                                                                "email_invalid",
                                                                                                999));
        assertThat(result.wasEmailInvalid()).isTrue();
    }

    @Test
    public void shouldCreateDeviceConflictFailure() {
        final Bundle loginBundle = new Bundle();
        AuthTaskResult result = AuthTaskResult.deviceConflict(loginBundle);
        assertThat(result.wasDeviceConflict()).isTrue();
        assertThat(result.getLoginBundle()).isSameAs(loginBundle);
    }

    @Test
    public void shouldCreateDeviceBlockFailure() {
        AuthTaskResult result = AuthTaskResult.deviceBlock();
        assertThat(result.wasDeviceBlock()).isTrue();
    }

    @Test
    public void shouldCreateValidationErrorResultFromApiRequestException() {
        final ApiRequestException failure = TestApiResponses.validationError().getFailure();
        final AuthTaskResult result = AuthTaskResult.failure(failure);
        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasValidationError()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo(failure.errorKey());
    }

    @Test
    public void shouldShowResponseBodyOnHandledError() {
        ApiResponse response = new ApiResponse(null, 400, "foobar");
        ApiRequestException error = ApiRequestException.serverError(null, response);
        AuthTaskResult result = AuthTaskResult.serverError(error);
        assertThat(result.toString()).contains("foobar");
    }

    @Test
    public void shouldShowExceptionMessageOnUnhandledError() {
        ApiRequestException error = ApiRequestException.networkError(null,
                                                                     new IOException(
                                                                             "somebody forgot to pay the mobile bill"));
        AuthTaskResult result = AuthTaskResult.failure(error);
        assertThat(result.toString()).contains("somebody forgot to pay the mobile bill");
    }

}
