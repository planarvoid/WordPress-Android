package com.soundcloud.android.tablet;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreenTablet;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TabletTest;

@TabletTest
public class MyProfileTest extends ActivityTestCase<MainActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
        profileScreen = new MenuScreenTablet(solo).clickProfile();
    }

    public void ignoreFollowButtonIsNotVisibleOnOwnProfile() {
        assertEquals("User profile should not have FOLLOW button", false, profileScreen.isFollowButtonVisible());
    }

    public void ignoreLocationIsShown() {
        assertEquals("Testville, United States", profileScreen.getLocation());
    }

}
