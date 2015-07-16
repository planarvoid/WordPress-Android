package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Bundle;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class AuthTaskResultTest {

    @Test
    public void shouldCreateSuccessResult() throws CreateModelException {
        PublicApiUser user = ModelFixtures.create(PublicApiUser.class);
        SignupVia signupVia = SignupVia.NONE;

        AuthTaskResult result = AuthTaskResult.success(user, signupVia, false);

        expect(result.wasSuccess()).toBeTrue();
        expect(result.getUser()).toEqual(user);
        expect(result.getSignupVia()).toEqual(signupVia);
    }

    @Test
    public void shouldCreateFailureResult() throws CreateModelException {
        String errorMessage = "Error Message Description";

        AuthTaskResult result = AuthTaskResult.failure(errorMessage);

        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldCreateEmailTakenResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.emailTaken(ApiRequestException.validationError(null, null, "email_taken", 999));
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldCreateSpamResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.spam(ApiRequestException.validationError(null, null, "spam", 999));
        expect(result.wasSpam()).toBeTrue();
    }

    @Test
    public void shouldCreateDeniedResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.denied(ApiRequestException.validationError(null, null, "denied", 999));
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldCreateEmailInvalidResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.emailInvalid(ApiRequestException.validationError(null, null, "email_invalid", 999));
        expect(result.wasEmailInvalid()).toBeTrue();
    }

    @Test
    public void shouldCreateDeviceConflictFailure() throws CreateModelException {
        final Bundle loginBundle = new Bundle();
        AuthTaskResult result = AuthTaskResult.deviceConflict(loginBundle);
        expect(result.wasDeviceConflict()).toBeTrue();
        expect(result.getLoginBundle()).toBe(loginBundle);
    }

    @Test
    public void shouldCreateValidationErrorResultFromApiRequestException() {
        final ApiRequestException failure = TestApiResponses.validationError().getFailure();
        final AuthTaskResult result = AuthTaskResult.failure(failure);
        expect(result.wasSuccess()).toBeFalse();
        expect(result.wasValidationError()).toBeTrue();
        expect(result.getServerErrorMessage()).toEqual(failure.errorKey());
    }

    @Test
    public void shouldShowResponseBodyOnHandledError() {
        ApiResponse response = new ApiResponse(null, 400, "foobar");
        ApiRequestException error = ApiRequestException.serverError(null, response);
        AuthTaskResult result = AuthTaskResult.serverError(error);
        expect(result.toString()).toContain("foobar");
    }

    @Test
    public void shouldShowExceptionMessageOnUnhandledError() {
            ApiRequestException error = ApiRequestException.networkError(null, new IOException("somebody forgot to pay the mobile bill"));
            AuthTaskResult result = AuthTaskResult.failure(error);
            expect(result.toString()).toContain("somebody forgot to pay the mobile bill");
    }
}
