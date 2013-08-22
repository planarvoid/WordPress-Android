package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class FetchUserTaskTest {

    @Test
    public void fetchLoadUserInfo() throws Exception {
        FetchUserTask task = new FetchUserTask(DefaultTestRunner.application.getCloudAPI());

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("../user.json"))));

        final User[] user = {null};
        FetchModelTask.Listener<User> listener = new FetchModelTask.Listener<User>() {
            @Override
            public void onSuccess(User u) {
                user[0] = u;
            }
            @Override
            public void onError(Object context) {
            }
        };

        task.addListener(listener);
        task.execute(Request.to(Endpoints.USER_DETAILS, 12345));
        expect(user[0]).not.toBeNull();
        expect(user[0].username).toEqual("SoundCloud Android @ MWC");
        expect(user[0].isPrimaryEmailConfirmed()).toBeFalse();

        User u = new UserStorage().getUser(3135930);
        expect(u).not.toBeNull();
        expect(u.username).toEqual("SoundCloud Android @ MWC");
    }
}
