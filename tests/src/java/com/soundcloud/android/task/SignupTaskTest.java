package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignupTaskTest extends ApiTests {
    @Test
    public void shouldReturnUser() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(201, resource("me.json"));
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        expect(u).not.toBeNull();
        expect(u.username).toEqual("testing");
    }

    @Test
    public void shouldProcessErrorsDuringSignup() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}");
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        expect(u).toBeNull();
        expect(task.mErrors).toEqual(Arrays.asList("Email has already been taken", "Email is already taken."));
    }

    @Test
    public void shouldProcessBadErrorResponse() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(422, "ada");
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        expect(u).toBeNull();
        expect(task.mErrors).toBeEmpty();
    }
}
