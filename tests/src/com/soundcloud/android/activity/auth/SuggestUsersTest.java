package com.soundcloud.android.activity.auth;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class SuggestUsersTest extends ApiTests {

    @Test @Ignore
    public void testConfigureFacebook() throws Exception {
        Robolectric.application.onCreate();

        SuggestedUsers users = new SuggestedUsers();

        addPendingHttpResponse(200, "[1,2,3]"); // followings
        users.onCreate(null);

        addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");
        users.configureFacebook();

        ShadowActivity shadow = shadowOf(users);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();
        assertThat(intent.intent.getData().toString(), equalTo("http://example.com"));
        assertThat(intent.intent.getComponent().getClassName(), equalTo(Connect.class.getName()));
    }

    @Test @Ignore
    public void testDoneButton() throws Exception {
        Robolectric.application.onCreate();

        SuggestedUsers users = new SuggestedUsers();
        addPendingHttpResponse(200, "[]"); // followings
        users.onCreate(null);

        users.findViewById(R.id.btn_done).performClick();
        assertThat(users.isFinishing(), is(true));
    }
}