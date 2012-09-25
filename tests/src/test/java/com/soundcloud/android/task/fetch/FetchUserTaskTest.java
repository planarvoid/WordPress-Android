package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class FetchUserTaskTest {
    FetchUserTask.FetchUserListener listener;

    @Test
    public void fetchLoadUserInfo() throws Exception {
        FetchUserTask task = new FetchUserTask(DefaultTestRunner.application, 0);

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("../user.json"))));

        User u = new User();
        u.id = 3135930;
        u.username = "old username";
        u.user_following = true;
        u.user_follower = true;
        SoundCloudApplication.MODEL_MANAGER.cache(u, false);

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
        expect(user[0]).not.toBeNull();
        expect(user[0].username).toEqual("SoundCloud Android @ MWC");
        expect(user[0].isPrimaryEmailConfirmed()).toBeFalse();


        u = SoundCloudApplication.MODEL_MANAGER.getUser(3135930);
        expect(u).not.toBeNull();
        expect(u.username).toEqual("SoundCloud Android @ MWC");

        u = SoundCloudApplication.MODEL_MANAGER.getCachedUser(3135930l);
        expect(u).not.toBeNull();
        expect(u.username).toEqual("SoundCloud Android @ MWC");
        expect(u.user_following).toBeTrue();
        expect(u.user_follower).toBeTrue();

    }
}
