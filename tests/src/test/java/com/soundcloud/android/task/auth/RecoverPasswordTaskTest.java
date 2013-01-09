package com.soundcloud.android.task.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class RecoverPasswordTaskTest {
    @Test
    public void shouldRequestPasswordReset() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(202, "");

        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application);
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfInvalidStatusIsReturned() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(404, "{\"error\":\"Unknown Email Address\"}");

        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application);
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeFalse();
        expect(task.getErrors()).toEqual(Arrays.asList("Unknown Email Address"));
    }

    @Test
    public void shouldReturnFalseIfTokenNotAvailable() throws Exception {
        Robolectric.addPendingHttpResponse(401, "unauthorized");
        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application);
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeFalse();
    }
}
