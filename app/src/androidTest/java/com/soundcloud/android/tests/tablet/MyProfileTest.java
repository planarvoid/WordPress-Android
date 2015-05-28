package com.soundcloud.android.tests.tablet;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreenTablet;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TabletTest;

@TabletTest
public class MyProfileTest extends ActivityTest<MainActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        profileScreen = new MenuScreenTablet(solo).clickUserProfile();
    }

    public void ignoreFollowButtonIsNotVisibleOnOwnProfile() {
        assertEquals("User profile should not have FOLLOW button", false, profileScreen.isFollowButtonVisible());
    }

    public void ignoreLocationIsShown() {
        assertEquals("Testville, United States", profileScreen.getLocation());
    }

}
