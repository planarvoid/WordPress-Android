package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;

public class FacebookInvitesTest extends TrackingActivityTest<LauncherActivity> {

    private static final String FACEBOOK_INVITES_CLOSED = "facebook-invites-closed";

    public FacebookInvitesTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.FACEBOOK_INVITES);
        ConfigurationHelper.forceFacebookInvitesNotification(getInstrumentation().getTargetContext());
        super.setUp();
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShouldHideFacebookInvitesNotification() {
        FacebookInvitesItemElement notification = new StreamScreen(solo)
                .getFirstFacebookInvitesNotification();

        startEventTracking();
        assertThat(notification.getViewElement(), is(visible()));
        notification.close();
        assertThat(notification.getViewElement(), is(not(visible())));
        finishEventTracking(FACEBOOK_INVITES_CLOSED);
    }

}
