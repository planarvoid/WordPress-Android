package com.soundcloud.android.activity.auth;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.objects.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.util.Pair;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class AddInfoTest extends RoboApiBaseTests {

    @Test
    public void shouldWorkWithNullFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("me.json"));
        AddInfo.AddUserInfoTask task = new AddInfo.AddUserInfoTask(realApi);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, (File)null));
        assertThat(result.username, equalTo("testing"));
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("me.json"));
        AddInfo.AddUserInfoTask task = new AddInfo.AddUserInfoTask(realApi);
        User user = new User();
        User result = task.doInBackground(Pair.create(user, new File("/tmp/bla")));
        assertThat(result.username, equalTo("testing"));
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("me.json"));
        AddInfo.AddUserInfoTask task = new AddInfo.AddUserInfoTask(realApi);
        User user = new User();

        File tmp = File.createTempFile("test", "tmp");
        User result = task.doInBackground(Pair.create(user, tmp));
        assertThat(result.username, equalTo("testing"));
    }
}
