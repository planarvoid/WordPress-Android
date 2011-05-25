package com.soundcloud.android.task;

import com.soundcloud.android.objects.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import com.xtremelabs.robolectric.Robolectric;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignupTaskTest extends RoboApiBaseTests {

    @Test
    public void shouldReturnUser() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("signup_token.json"));
        Robolectric.addPendingHttpResponse(201, slurp("me.json"));
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        assertThat(u, CoreMatchers.<Object>notNullValue());
        assertThat(u.username, equalTo("jberkel_testing"));
    }

    @Test
    public void shouldProcessErrorsDuringSignup() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("signup_token.json"));
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}");
        SignupTask task = new SignupTask(api);
        User u = task.doInBackground("email", "password");
        assertThat(u, CoreMatchers.<Object>nullValue());
        assertThat(task.errors, equalTo(Arrays.asList("Email has already been taken", "Email is already taken.")));
    }
}
