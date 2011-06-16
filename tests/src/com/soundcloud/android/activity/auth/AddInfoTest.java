package com.soundcloud.android.activity.auth;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AddInfoTest extends ApiTests {

    @Test
    public void testAddUserInfoSuccess() throws Exception {
        Robolectric.application.onCreate();
        AddInfo info = new AddInfo();
        User user = new User();

        addPendingHttpResponse(200, resource("user.json"));
        User result = info.addUserInfo(user, "foobaz", null);

        assertThat(result.username, equalTo("foobaz"));
        assertThat(result.permalink, equalTo("foobaz"));
        assertThat(info.isFinishing(), is(true));
    }

    @Test
    public void testAddUserInfoFail() throws Exception {
        Robolectric.application.onCreate();
        AddInfo info = new AddInfo();

        addPendingHttpResponse(422, "{\"error\":\"Failz\"}");
        info.addUserInfo(new User(), "foobaz", null);
        assertThat(info.isFinishing(), is(false));
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo("Error adding info: Failz"));
    }
}
