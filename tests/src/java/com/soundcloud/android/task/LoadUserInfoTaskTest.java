package com.soundcloud.android.task;

import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.soundcloud.android.utils.CloudUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class LoadUserInfoTaskTest {
    LoadUserInfoTask.LoadUserInfoListener listener;

    @Test
    public void testLoadTrackInfo() throws Exception {
        LoadUserInfoTask task = new LoadUserInfoTask(DefaultTestRunner.application, 0, true, true);

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("user.json"))));

        final User[] user = {null};
        listener = new LoadUserInfoTask.LoadUserInfoListener() {
            @Override
            public void onUserInfoLoaded(User t) {
                user[0] = t;
            }
            @Override
            public void onUserInfoError(long id) {
            }
        };

        task.setListener(listener);
        task.execute(Request.to(Endpoints.USER_DETAILS, 12345));
        assertThat(user[0], not(nullValue()));
        assertThat(user[0].username, equalTo("SoundCloud Android @ MWC"));
    }
}
