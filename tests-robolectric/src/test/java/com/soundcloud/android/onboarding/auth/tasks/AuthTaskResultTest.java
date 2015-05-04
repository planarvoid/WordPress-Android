package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Bundle;

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
        AuthTaskResult result = AuthTaskResult.emailTaken();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldCreateSpamResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.spam();
        expect(result.wasSpam()).toBeTrue();
    }

    @Test
    public void shouldCreateDeniedResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.denied();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldCreateEmailInvalidResult() throws CreateModelException {
        AuthTaskResult result = AuthTaskResult.emailInvalid();
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
}
