package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.Robolectric;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.util.Pair;

import java.io.File;
import java.util.Arrays;

@RunWith(DefaultTestRunner.class)
public class AddUserInfoTaskTest extends ApiTests {
    @Test
    public void shouldWorkWithNullFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("me.json"));
        AddUserInfoTask task = new AddUserInfoTask(api);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, (File)null));
        assertThat(result.username, equalTo("testing"));
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("me.json"));
        AddUserInfoTask task = new AddUserInfoTask(api);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, new File("/tmp/bla")));
        assertThat(result.username, equalTo("testing"));
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("me.json"));
        AddUserInfoTask task = new AddUserInfoTask(api);
        User user = new User();
        File tmp = File.createTempFile("test", "tmp");
        User result = task.doInBackground(Pair.create(user, tmp));
        assertThat(result.username, equalTo("testing"));
    }

    @Test
    public void shouldHandleBadEntity() throws Exception {
        Robolectric.addPendingHttpResponse(422, "{\"errors\":{\"error\":\"Failz\"}}");
        AddUserInfoTask task = new AddUserInfoTask(api);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, (File)null));
        assertThat(result, CoreMatchers.<Object>nullValue());
        assertThat(task.mErrors, equalTo(Arrays.asList("Failz")));
    }
}
