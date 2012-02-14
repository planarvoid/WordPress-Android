package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class FetchUserInfoTaskTest {
    FetchUserTask.FetchUserListener listener;

    @Test
    public void fetchLoadTrackInfo() throws Exception {
        FetchUserTask task = new FetchUserTask(DefaultTestRunner.application, 0);

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("user.json"))));

        User u = new User();
        u.id = 3135930;
        u.username = "old username";
        u.user_following = true;
        u.user_follower = true;
        ((SoundCloudApplication) Robolectric.application).USER_CACHE.put(u);

        final User[] user = {null};
        listener = new FetchUserTask.FetchUserListener() {
            @Override
            public void onSuccess(User u, String action) {
                user[0] = u;
            }
            @Override
            public void onError(long id) {
            }
        };

        task.addListener(listener);
        task.execute(Request.to(Endpoints.USER_DETAILS, 12345));
        assertThat(user[0], not(nullValue()));
        assertThat(user[0].username, equalTo("SoundCloud Android @ MWC"));


        u = SoundCloudDB.getUserById(Robolectric.application.getContentResolver(), 3135930);
        assertThat(u, not(nullValue()));
        assertThat(u.username, equalTo("SoundCloud Android @ MWC"));

        u = ((SoundCloudApplication) Robolectric.application).USER_CACHE.get(3135930l);
        assertThat(u, not(nullValue()));
        assertThat(u.username, equalTo("SoundCloud Android @ MWC"));
        assertThat(u.user_following, equalTo(true));
        assertThat(u.user_follower, equalTo(true));

    }
}
