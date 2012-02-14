package com.soundcloud.android.activity.auth;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignupDetailsTest extends ApiTests {

    @Test
    public void testAddUserInfoSuccess() throws Exception {
        SignupDetails info = new SignupDetails();
        User user = new User();

        addPendingHttpResponse(200, resource("user.json"));
        User result = info.addUserInfo(user, "foobaz", null);

        assertThat(result.username, equalTo("foobaz"));
        assertThat(result.permalink, equalTo("foobaz"));
        assertThat(info.isFinishing(), is(true));

        ShadowActivity activity = shadowOf(info);
        assertThat(activity.getResultCode(), is(-1));
    }

    @Test
    @DisableStrictI18n
    public void testAddUserInfoFail() throws Exception {
        SignupDetails info = new SignupDetails();

        addPendingHttpResponse(422, "{\"error\":\"Failz\"}");
        info.addUserInfo(new User(), "foobaz", null);
        assertThat(info.isFinishing(), is(false));
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo("Error adding info: Failz"));
    }
}
