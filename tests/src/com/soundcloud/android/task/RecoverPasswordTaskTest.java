package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class RecoverPasswordTaskTest extends ApiTests {
    @Test
    public void shouldRequestPasswordReset() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(202, "");

        RecoverPasswordTask task = new RecoverPasswordTask(api);
        Boolean result = task.doInBackground("foo@gmail.com");
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseIfInvalidStatusIsReturned() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("signup_token.json"));
        Robolectric.addPendingHttpResponse(404, "{\"error\":\"Unknown Email Address\"}");

        RecoverPasswordTask task = new RecoverPasswordTask(api);
        Boolean result = task.doInBackground("foo@gmail.com");
        assertThat(result, is(false));
        assertThat(task.mErrors, equalTo(Arrays.asList("Unknown Email Address")));
    }

    @Test
    public void shouldReturnFalseIfTokenNotAvailable() throws Exception {
        Robolectric.addPendingHttpResponse(401, "unauthorized");
        RecoverPasswordTask task = new RecoverPasswordTask(api);
        Boolean result = task.doInBackground("foo@gmail.com");
        assertThat(result, is(false));
    }
}
