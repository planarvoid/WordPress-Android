package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OtherProfileErrorTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen profileScreen;

    public OtherProfileErrorTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = new MenuScreen(solo)
                .open()
                .clickUserProfile()
                .touchFollowingsTab();

        networkManagerClient.switchWifiOff();

        profileScreen.getUsers().get(0).click();
    }

    public void testConnectionErrorAndRetryInPosts() {
        assertTrue(profileScreen.emptyConnectionErrorMessage().isVisible());

        networkManagerClient.switchWifiOn();
        profileScreen.retryFromErrorView();

        assertTrue(profileScreen.playTrack(0).isVisible());
    }
}
