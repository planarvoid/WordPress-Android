package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.properties.Flag;

public class EmptyTrackLikesTest extends TrackLikesTest {

    @Override
    public void setUp() throws Exception {
        emptyUser.logIn(getInstrumentation().getTargetContext());
        setDependsOn(Flag.NEW_LIKES_END_TO_END);
        super.setUp();
    }

    public void testShowsEmptyLikesScreen() {
        assertTrue(likesScreen.emptyView().isVisible());
    }

}
