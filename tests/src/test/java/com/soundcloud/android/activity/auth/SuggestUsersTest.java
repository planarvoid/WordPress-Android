package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SuggestUsersTest {

    @Test @Ignore
    public void testConfigureFacebook() throws Exception {
        SuggestedUsers users = new SuggestedUsers();

        addPendingHttpResponse(200, "[1,2,3]"); // followings
        users.onCreate(null);

        addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");
        users.configureFacebook();

        ShadowActivity shadow = shadowOf(users);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();
        expect(intent.intent.getData().toString()).toEqual("http://example.com");
        expect(intent.intent.getComponent().getClassName()).toEqual(Connect.class.getName());
    }

    @Test @Ignore
    public void testDoneButton() throws Exception {
        SuggestedUsers users = new SuggestedUsers();
        addPendingHttpResponse(200, "[]"); // followings
        users.onCreate(null);

        users.findViewById(R.id.btn_done).performClick();
        expect(users.isFinishing()).toBeTrue();
    }
}