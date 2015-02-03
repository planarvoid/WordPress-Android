package com.soundcloud.android.tests.likes;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OfflineTrackLikesTest extends ActivityTest<MainActivity> {

    public OfflineTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        ConfigurationHelper.enableOfflineSync(getActivity());
    }

    public void testLikesScreenWithoutOfflineLikesEnabled() {
        LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), getWaiter());
        assertFalse(likesScreen.isSyncIconVisible());

        assertTrue(likesScreen.actionBar().syncAction().isVisible());
    }

}
