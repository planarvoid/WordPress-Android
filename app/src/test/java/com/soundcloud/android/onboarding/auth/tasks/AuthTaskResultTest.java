package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class AuthTaskResultTest {

    @Test
    public void shouldCreateSuccessResult() throws CreateModelException {
        PublicApiUser user = TestHelper.getModelFactory().createModel(PublicApiUser.class);
        SignupVia signupVia = SignupVia.NONE;

        AuthTaskResult result = AuthTaskResult.success(user, signupVia);

        expect(result.wasSuccess()).toBeTrue();
        expect(result.getUser()).toEqual(user);
        expect(result.getSignupVia()).toEqual(signupVia);
    }

    @Test
    public void shouldCreateFailureResult() throws CreateModelException {
        String errorMessage = "Error Message Description";

        AuthTaskResult result = AuthTaskResult.failure(errorMessage);

        expect(result.wasFailure()).toBeTrue();
        expect(Arrays.equals(result.getErrors(), new String[] { errorMessage })).toBeTrue();
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

}