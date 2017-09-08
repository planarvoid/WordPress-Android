package com.soundcloud.android.tests.stream;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.forceFacebookListenerInvitesNotification;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class FacebookInvitesTest extends ActivityTest<LauncherActivity> {

    private static final String FACEBOOK_INVITES_CLOSED = "specs/facebook-invites-closed.spec";

    public FacebookInvitesTest() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        forceFacebookListenerInvitesNotification(getInstrumentation().getTargetContext());
        super.setUp();
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
    }

    @Test
    public void testShouldHideFacebookInvitesNotification() throws Exception {
        FacebookInvitesItemElement notification = new StreamScreen(solo)
                .getFirstFacebookInvitesNotification();

        assertThat(notification.getViewElement(), is(visible()));
        notification.close();
        assertThat(notification.getViewElement(), is(not(visible())));

        mrLocalLocal.verify(FACEBOOK_INVITES_CLOSED);
    }

}
