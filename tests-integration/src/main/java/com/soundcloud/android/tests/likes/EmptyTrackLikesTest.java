package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.emptyUser;

public class EmptyTrackLikesTest extends TrackLikesTest {

    @Override
    public void setUp() throws Exception {
        emptyUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testShowsEmptyLikesScreen() {
        assertTrue(likesScreen.emptyView().isVisible());
    }

}
