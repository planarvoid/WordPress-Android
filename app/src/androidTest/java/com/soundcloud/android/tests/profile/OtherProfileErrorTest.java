package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OtherProfileErrorTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen profileScreen;

    public OtherProfileErrorTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return profileEntryUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile()
                                     .touchFollowingsTab();

        networkManagerClient.switchWifiOff();

        profileScreen.getUsers().get(0).click();
    }

    // TODO: this test seems pretty flaky but it is pretty much the same code than before
    public void testConnectionErrorAndRetryInPosts() {
        assertTrue(profileScreen.emptyConnectionErrorMessage().isOnScreen());

        networkManagerClient.switchWifiOn();
        profileScreen.pullToRefresh();

        assertThat(profileScreen.currentItemCount(), is(greaterThan(0)));
    }
}
