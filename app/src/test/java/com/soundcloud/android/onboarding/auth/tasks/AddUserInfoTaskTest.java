package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class AddUserInfoTaskTest  {
    @Test
    public void shouldWorkWithNullFile() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "me.json");

        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application, "name", null);
        AuthTaskResult result = task.doInBackground();
        expect(result.getUser().username).toEqual("testing");
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "me.json");

        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application, "name", new File("/tmp/bla"));
        AuthTaskResult result = task.doInBackground();
        expect(result.getUser().username).toEqual("testing");
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "me.json");

        File tmp = File.createTempFile("test", "tmp");
        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application, "name", tmp);
        AuthTaskResult result = task.doInBackground();
        expect(result.getUser().username).toEqual("testing");
    }

    @Test
    public void shouldHandleBadEntity() throws Exception {
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":\"Failz\"}}");

        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application, "name", null);
        AuthTaskResult result = task.doInBackground();
        expect(result.getUser()).toBeNull();
        expect(result.getErrors()[0]).toEqual("Failz");
    }
}
