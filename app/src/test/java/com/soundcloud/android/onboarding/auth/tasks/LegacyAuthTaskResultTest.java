package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Bundle;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class LegacyAuthTaskResultTest {

    @Test
    public void shouldCreateSuccessResult() throws CreateModelException {
        ApiUser user = ModelFixtures.create(ApiUser.class);
        SignupVia signupVia = SignupVia.NONE;

        LegacyAuthTaskResult result = LegacyAuthTaskResult.success(user, signupVia);

        assertThat(result.wasSuccess()).isTrue();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getSignupVia()).isEqualTo(signupVia);
    }

    @Test
    public void shouldCreateFailureResult() {
        String errorMessage = "Error Message Description";

        LegacyAuthTaskResult result = LegacyAuthTaskResult.failure(errorMessage);

        assertThat(result.wasFailure()).isTrue();
    }

    @Test
    public void shouldCreateEmailTakenResult() {
        LegacyAuthTaskResult result = LegacyAuthTaskResult.emailTaken(ApiRequestException.validationError(null,
                                                                                                          null,
                                                                                                          "email_taken",
                                                                                                          999));
        assertThat(result.wasEmailTaken()).isTrue();
    }

    @Test
    public void shouldCreateSpamResult() {
        LegacyAuthTaskResult result = LegacyAuthTaskResult.spam(ApiRequestException.validationError(null, null, "spam", 999));
        assertThat(result.wasSpam()).isTrue();
    }

    @Test
    public void shouldCreateDeniedResult() {
        LegacyAuthTaskResult result = LegacyAuthTaskResult.denied(ApiRequestException.validationError(null, null, "denied", 999));
        assertThat(result.wasDenied()).isTrue();
    }

    @Test
    public void shouldCreateEmailInvalidResult() {
        LegacyAuthTaskResult result = LegacyAuthTaskResult.emailInvalid(ApiRequestException.validationError(null,
                                                                                                            null,
                                                                                                            "email_invalid",
                                                                                                            999));
        assertThat(result.wasEmailInvalid()).isTrue();
    }

    @Test
    public void shouldCreateDeviceConflictFailure() {
        final Bundle loginBundle = new Bundle();
        LegacyAuthTaskResult result = LegacyAuthTaskResult.deviceConflict(loginBundle);
        assertThat(result.wasDeviceConflict()).isTrue();
        assertThat(result.getLoginBundle()).isSameAs(loginBundle);
    }

    @Test
    public void shouldCreateDeviceBlockFailure() {
        LegacyAuthTaskResult result = LegacyAuthTaskResult.deviceBlock();
        assertThat(result.wasDeviceBlock()).isTrue();
    }

    @Test
    public void shouldCreateValidationErrorResultFromApiRequestException() {
        final ApiRequestException failure = TestApiResponses.validationError().getFailure();
        final LegacyAuthTaskResult result = LegacyAuthTaskResult.failure(failure);
        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasValidationError()).isTrue();
        assertThat(result.getServerErrorMessage()).isEqualTo(failure.errorKey());
    }

    @Test
    public void shouldShowResponseBodyOnHandledError() {
        ApiResponse response = new ApiResponse(null, 400, "foobar");
        ApiRequestException error = ApiRequestException.serverError(null, response);
        LegacyAuthTaskResult result = LegacyAuthTaskResult.serverError(error);
        assertThat(result.toString()).contains("foobar");
    }

    @Test
    public void shouldShowExceptionMessageOnUnhandledError() {
        ApiRequestException error = ApiRequestException.networkError(null,
                                                                     new IOException(
                                                                             "somebody forgot to pay the mobile bill"));
        LegacyAuthTaskResult result = LegacyAuthTaskResult.failure(error);
        assertThat(result.toString()).contains("somebody forgot to pay the mobile bill");
    }

}
