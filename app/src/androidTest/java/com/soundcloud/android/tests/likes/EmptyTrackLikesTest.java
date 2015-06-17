package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyTrackLikesTest extends ActivityTest<MainActivity> {

    protected TrackLikesScreen likesScreen;

    public EmptyTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyLikesScreen() {
        menuScreen = new MenuScreen(solo);
        likesScreen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(likesScreen.emptyView().isVisible());
    }
}
