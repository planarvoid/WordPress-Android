package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class RecoverPasswordTaskTest {
    @Test
    public void shouldRequestPasswordReset() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(202, "");

        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application.getCloudAPI());
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfInvalidStatusIsReturned() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "signup_token.json");
        Robolectric.addPendingHttpResponse(404, "{\"error\":\"Unknown Email Address\"}");

        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application.getCloudAPI());
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeFalse();
    }

    @Test
    public void shouldReturnFalseIfTokenNotAvailable() throws Exception {
        Robolectric.addPendingHttpResponse(401, "unauthorized");
        RecoverPasswordTask task = new RecoverPasswordTask(DefaultTestRunner.application.getCloudAPI());
        Boolean result = task.doInBackground("foo@gmail.com");
        expect(result).toBeFalse();
    }
}
