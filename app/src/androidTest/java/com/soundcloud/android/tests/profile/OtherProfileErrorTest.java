package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OtherProfileErrorTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen screen;

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

        menuScreen = new MenuScreen(solo);
        screen = menuScreen.open().clickUserProfile();
        networkManagerClient.switchWifiOff();
        screen.touchFollowingsTab();
        screen = screen.getUsers()
                .get(0)
                .click();
    }

    public void testConnectionErrorAndRetryInPosts() {
        assertTrue(screen.emptyConnectionErrorMessage().isVisible());

        networkManagerClient.switchWifiOn();
        screen.retryFromErrorView();

        assertTrue(screen.playTrack(0).isVisible());
    }
}
