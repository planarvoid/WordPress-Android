package com.soundcloud.android.task.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.TokenInformationGenerator;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
        signupTask = new SignupTask(DefaultTestRunner.application, tokenInformationGenerator, userStorage);
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
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}");
        AuthTaskResult result = signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());
        expect(result.wasSuccess()).toBeFalse();
        String[] errors = result.getErrors();
        expect(Arrays.equals(errors, new String[]{"Email has already been taken", "Email is already taken."})).toBeTrue();
    }

    @Test
    public void shouldProcessBadErrorResponse() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "ada");
        AuthTaskResult result = signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());
        expect(result.wasSuccess()).toBeFalse();
        expect(Arrays.equals(result.getErrors(), new String[]{})).toBeTrue();
    }

    @Test
    public void shouldHandleRevokedSignupScope() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token_blank_scope.json");
        AuthTaskResult result = signupTask.doSignup(DefaultTestRunner.application, getParamsBundle());
        expect(result.wasSuccess()).toBeFalse();
        String[] signupScopeError = {DefaultTestRunner.application.getString(R.string.signup_scope_revoked)};
        String[] errors = result.getErrors();
        expect(Arrays.equals(errors, signupScopeError)).toBeTrue();
    }

    @Test
    public void shouldHandleErrorCodeFromTokenController() throws Exception {
        // we get a token which has signup scope, but still returns forbidden on token request

        TestHelper.addPendingHttpResponse(getClass(), ("signup_token.json"));
        Robolectric.addPendingHttpResponse(403, "",
            new BasicHeader("WWW-Authenticate", "OAuth realm=\"SoundCloud\", error=\"insufficient_scope\""));

        SignupTask task = signupTask;
        AuthTaskResult result = task.doSignup(DefaultTestRunner.application, getParamsBundle());
        expect(result.wasSuccess()).toBeFalse();
        String[] signupScopeError = {DefaultTestRunner.application.getString(R.string.signup_scope_revoked)};
        expect(Arrays.equals(result.getErrors(), signupScopeError)).toBeTrue();
    }
}
