package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignupTaskTest {
    @Test
    public void shouldReturnUser() throws Exception {
        TestHelper.addCannedResponses(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(201, TestHelper.resourceAsString(getClass(), "me.json"));
        SignupTask task = new SignupTask(DefaultTestRunner.application);
        User u = task.doInBackground("email", "password");
        expect(u).not.toBeNull();
        expect(u.username).toEqual("testing");
    }

    @Test
    public void shouldProcessErrorsDuringSignup() throws Exception {
        TestHelper.addCannedResponses(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}");
        SignupTask task = new SignupTask(DefaultTestRunner.application);
        expect(task.doInBackground("email", "password")).toBeNull();
        expect(task.mErrors).toEqual(Arrays.asList("Email has already been taken", "Email is already taken."));
    }

    @Test
    public void shouldProcessBadErrorResponse() throws Exception {
        TestHelper.addCannedResponses(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(422, "ada");
        SignupTask task = new SignupTask(DefaultTestRunner.application);
        expect(task.doInBackground("email", "password")).toBeNull();
        expect(task.mErrors).toBeEmpty();
    }

    @Test
    public void shouldHandleRevokedSignupScope() throws Exception {
        TestHelper.addCannedResponses(getClass(), "signup_token_blank_scope.json");
        SignupTask task = new SignupTask(DefaultTestRunner.application);
        expect(task.doInBackground("email", "password")).toBeNull();
        expect(task.mErrors).toContain(DefaultTestRunner.application.getString(R.string.signup_scope_revoked));
    }

    @Test
    public void shouldHandleErrorCodeFromTokenController() throws Exception {
        // we get a token which has signup scope, but still returns forbidden on token request

        TestHelper.addCannedResponses(getClass(), ("signup_token.json"));
        Robolectric.addPendingHttpResponse(403, "",
            new BasicHeader("WWW-Authenticate", "OAuth realm=\"SoundCloud\", error=\"insufficient_scope\""));

        SignupTask task = new SignupTask(DefaultTestRunner.application);
        expect(task.doInBackground("email", "password")).toBeNull();
        expect(task.mErrors).toContain(DefaultTestRunner.application.getString(R.string.signup_scope_revoked));
    }
}
