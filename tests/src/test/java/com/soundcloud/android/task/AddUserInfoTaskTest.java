package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.util.Pair;

import java.io.File;
import java.util.Arrays;

@RunWith(DefaultTestRunner.class)
public class AddUserInfoTaskTest  {
    @Test
    public void shouldWorkWithNullFile() throws Exception {
        TestHelper.addCannedResponses(getClass(), "me.json");

        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, (File)null));
        expect(result.username).toEqual("testing");
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        TestHelper.addCannedResponses(getClass(), "me.json");
        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, new File("/tmp/bla")));
        expect(result.username).toEqual("testing");
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        TestHelper.addCannedResponses(getClass(), "me.json");
        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application);
        User user = new User();
        File tmp = File.createTempFile("test", "tmp");
        User result = task.doInBackground(Pair.create(user, tmp));
        expect(result.username).toEqual("testing");
    }

    @Test
    public void shouldHandleBadEntity() throws Exception {
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":\"Failz\"}}");
        AddUserInfoTask task = new AddUserInfoTask(DefaultTestRunner.application);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, (File)null));
        expect(result).toBeNull();
        expect(task.mErrors).toEqual(Arrays.asList("Failz"));
    }
}
