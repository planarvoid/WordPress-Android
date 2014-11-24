package com.soundcloud.android.tests.tablet;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.screens.MenuScreenTablet;
import com.soundcloud.android.framework.screens.ProfileScreen;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TabletTest;

@TabletTest
public class MyProfileTest extends ActivityTest<MainActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
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
