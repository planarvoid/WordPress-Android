package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class SignupTaskTest {

    private SignupTask signupTask;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private LegacyUserStorage userStorage;

    @Before
    public void setUp() throws Exception {
        signupTask = new SignupTask(DefaultTestRunner.application, tokenInformationGenerator, userStorage,
                DefaultTestRunner.application.getCloudAPI());
    }

    @Test
    public void shouldReturnUser() throws Exception {
        setupPendingHttpResponse(201, JsonFixtures.resourceAsString(getClass(), "me.json"));

        AuthTaskResult result = doSignup();

        expect(result.getUser()).not.toBeNull();
        expect(result.getUser().username).toEqual("testing");
    }

    @Test
    public void shouldProcessLegacyErrorArrayOfNewResponseBodyDuringSignup() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 101, \"errors\": [{ \"error_message\":\"Email has already been taken\"},{\"error_message\":\"Address has already been taken\"}] }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldProcessLegacyErrorObjectOfNewResponseBodyDuringSignup() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 101, \"errors\": {\"error\": \"Email is taken\"}  }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldReturnEmailTakenAuthTaskResultOnSignupEmailTakenError() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 101 }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldReturnEmailTakenAuthTaskResultOnSignupEmailTakenErrorWithLegacyErrors() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 101, \"errors\": [{ \"error_message\": \"Email has already been taken.\" }] }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailTaken()).toBeTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupDomainBlacklistedError() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 102 }");
        AuthTaskResult result = doSignup();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupDomainBlacklistedErrorWithLegacyErrors() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 102, \"errors\": [{ \"error_message\": \"Email domain is blacklisted.\" }] }");
        AuthTaskResult result = doSignup();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldReturnSpamAuthTaskResultOnSignupCaptchaRequiredError() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": \"103\" }");
        AuthTaskResult result = doSignup();
        expect(result.wasSpam()).toBeTrue();
    }

    @Test
    public void shouldReturnSpamAuthTaskResultOnSignupCaptchaRequiredErrorWithLegacyErrors() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 103, \"errors\": [{ \"error_message\": \"Spam detected, login on web page with captcha.\" }] }");
        AuthTaskResult result = doSignup();
        expect(result.wasSpam()).toBeTrue();
    }

    @Test
    public void shouldReturnEmailInvalidAuthTaskResultOnSignupEmailInvalidError() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 104 }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailInvalid()).toBeTrue();
    }

    @Test
    public void shouldReturnEmailInvalidAuthTaskResultOnSignupEmailInvalidErrorWithLegacyErrors() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 104, \"errors\": [{ \"error_message\": \"Email is invalid.\" }] }");
        AuthTaskResult result = doSignup();
        expect(result.wasEmailInvalid()).toBeTrue();
    }

    @Test
    public void shouldReturnGenericErrorAuthTaskResultOnSignupOtherError() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 105 }");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnGenericErrorAuthTaskResultOnSignupOtherErrorWithLegacyErrors() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": 105, \"errors\": [{ \"error_message\": \"Sorry we couldn't sign you up with the details you provided.\" }] }");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnUnrecognizedErrorCode() throws Exception {
        setupPendingHttpResponse(422, "{ \"error\": \"180\" }");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupWithUnreconizedError() throws Exception {
        setupPendingHttpResponse(422, "{ \"unrecognized\": \"error\" }");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnDeniedAuthTaskResultOnSignupForbidden() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(403, "",
                new BasicHeader("WWW-Authenticate", "OAuth realm=\"SoundCloud\", error=\"insufficient_scope\""));
        AuthTaskResult result = doSignup();
        expect(result.wasDenied()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupServerError() throws Exception {
        setupPendingHttpResponse(500, "");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    @Test
    public void shouldReturnFailureAuthTaskResultOnSignupUnexpectedResponseStatus() throws Exception {
        setupPendingHttpResponse(102, "");
        AuthTaskResult result = doSignup();
        expect(result.wasFailure()).toBeTrue();
    }

    private AuthTaskResult doSignup() {
        return signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());
    }

    private Bundle getParamsBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SignupTask.KEY_USERNAME, "username");
        bundle.putString(SignupTask.KEY_PASSWORD, "password");
        bundle.putSerializable(SignupTask.KEY_BIRTHDAY, BirthdayInfo.buildFrom(22));
        bundle.putString(SignupTask.KEY_GENDER, "fluid");
        return bundle;
    }

    private void setupPendingHttpResponse(int statusCode, String responseBody) throws IOException {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(statusCode, responseBody);
    }

}
