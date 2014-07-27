package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.UserStorage;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignupTaskTest {

    private SignupTask signupTask;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private UserStorage userStorage;

    @Before
    public void setUp() throws Exception {
        signupTask = new SignupTask(DefaultTestRunner.application, tokenInformationGenerator, userStorage,
                DefaultTestRunner.application.getCloudAPI());
    }

    @Test
    public void shouldReturnUser() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(201, TestHelper.resourceAsString(getClass(), "me.json"));

        AuthTaskResult result = signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());

        expect(result.getUser()).not.toBeNull();
        expect(result.getUser().username).toEqual("testing");
    }

    private Bundle getParamsBundle() {
        Bundle b = new Bundle();
        b.putString(SignupTask.KEY_USERNAME, "username");
        b.putString(SignupTask.KEY_PASSWORD,"password");
        return b;
    }

    @Test
    public void shouldProcessErrorsDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken\"]}}");
        signUpAndExpectErrorsInResponseBody(new String[]{"Email has already been taken", "Email is already taken"});
    }

    @Test
    public void shouldProcessMultipleErrorsOfNewResponseBodyDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{ \"error\": 101, \"email\": \"taken\", \"errors\": [{ \"error_message\":\"Email has already been taken\"},{\"error_message\":\"Address has already been taken\"}] }");
        signUpAndExpectErrorsInResponseBody(new String[]{"Email has already been taken", "Address has already been taken"});
    }

    @Test
    public void shouldProcessErrorsOfNewResponseBodyForExistingEmailDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{ \"error\": 101, \"email\": \"taken\", \"errors\": [{ \"error_message\": \"Email has already been taken.\" }] }");
        signUpAndExpectErrorsInResponseBody(new String[]{"Email has already been taken."});
    }

    @Test
    public void shouldProcessErrorsOfNewResponseBodyForDomainBlacklistedDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{ \"error\": 102, \"email\": \"domain_blacklisted\", \"errors\": [{ \"error_message\": \"Email domain is blacklisted.\" }] }");
        signUpAndExpectErrorsInResponseBody(new String[]{"Email domain is blacklisted."});
    }

    @Test
    public void shouldProcessErrorsOfNewResponseBodyForCaptchaChallengeDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{ \"error\": 103, \"captcha_challenge_response\": \"required\", \"errors\": [{ \"error_message\": \"Spam detected, login on web page with captcha.\" }] }");
        signUpAndExpectErrorsInResponseBody(new String[]{"Spam detected, login on web page with captcha."});
    }

    @Test
    public void shouldProcessErrorsOfNewResponseBodyInvalidEmailDuringSignup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{ \"error\": 104, \"email\": \"is invalid\", \"errors\": [{ \"error_message\": \"Email is invalid.\" }] }");
        signUpAndExpectErrorsInResponseBody(new String[]{"Email is invalid."});
    }

    @Test
    public void shouldProcessBadErrorResponse() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "ada");
        signUpAndExpectErrorsInResponseBody(new String[]{});
    }

    @Test
    public void shouldHandleRevokedSignupScope() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token_blank_scope.json");
        signUpAndExpectErrorsInResponseBody(new String[]{DefaultTestRunner.application.getString(R.string.signup_scope_revoked)});
    }

    @Test
    public void shouldHandleErrorCodeFromTokenController() throws Exception {
        // we get a token which has signup scope, but still returns forbidden on token request
        TestHelper.addPendingHttpResponse(getClass(), ("signup_token.json"));
        Robolectric.addPendingHttpResponse(403, "",
            new BasicHeader("WWW-Authenticate", "OAuth realm=\"SoundCloud\", error=\"insufficient_scope\""));
        signUpAndExpectErrorsInResponseBody(new String[]{DefaultTestRunner.application.getString(R.string.signup_scope_revoked)});
    }

    private void signUpAndExpectErrorsInResponseBody(String[] expectedErrors) {
        AuthTaskResult result = signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());
        expect(result.wasSuccess()).toBeFalse();
        String[] errors = result.getErrors();
        expect(Arrays.equals(errors, expectedErrors)).toBeTrue();
    }
}
