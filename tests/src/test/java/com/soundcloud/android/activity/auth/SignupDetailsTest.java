package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignupDetailsTest  {

    @Test
    public void testAddUserInfoSuccess() throws Exception {
        SignupDetails info = new SignupDetails();
        User user = new User();

        TestHelper.addCannedResponses(getClass(), "user.json");
        User result = info.addUserInfo(user, "foobaz", null);

        expect(result.username).toEqual("foobaz");
        expect(result.permalink).toEqual("foobaz");
        expect(info.isFinishing()).toBeTrue();

        ShadowActivity activity = shadowOf(info);
        expect(activity.getResultCode()).toEqual(-1);
    }

    @Test
    @DisableStrictI18n
    public void testAddUserInfoFail() throws Exception {
        SignupDetails info = new SignupDetails();

        addPendingHttpResponse(422, "{\"error\":\"Failz\"}");
        info.addUserInfo(new User(), "foobaz", null);
        expect(info.isFinishing()).toBeFalse();
        expect(ShadowToast.getTextOfLatestToast()).toEqual("Error adding info: Failz");
    }
}
