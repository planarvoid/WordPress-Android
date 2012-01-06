package com.soundcloud.android.task;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.Robolectric;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignupTaskTest extends ApiTests {
    @Test
    public void shouldReturnUser() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(201, resource("me.json"));
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        assertThat(u, CoreMatchers.<Object>notNullValue());
        assertThat(u.username, equalTo("testing"));
    }

    @Test
    public void shouldProcessErrorsDuringSignup() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}");
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        assertThat(u, CoreMatchers.<Object>nullValue());
        assertThat(task.mErrors, equalTo(Arrays.asList("Email has already been taken", "Email is already taken.")));
    }

    @Test
    public void shouldProcessBadErrorResponse() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(422, "ada");
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        assertThat(u, CoreMatchers.<Object>nullValue());
        assertThat(task.mErrors.isEmpty(), is(true));
    }
}
