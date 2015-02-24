package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyTrackLikesTest extends ActivityTest<MainActivity> {

    protected LikesScreen likesScreen;

    public EmptyTrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        emptyUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testShowsEmptyLikesScreen() {
        menuScreen = new MenuScreen(solo);
        likesScreen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(likesScreen.emptyView().isVisible());
    }
}
