package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class TrackLikesPlaybackTest extends TrackLikesTest {

    @Override
    public void setUp() throws Exception {
        playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testClickingShuffleButtonOpensPlayer() {
        VisualPlayerElement playerElement = likesScreen.clickShuffleButton();

        assertThat(playerElement, is(visible()));
    }

    public void testClickingTrackOpensPlayer() {
        VisualPlayerElement playerElement = likesScreen.clickItem(1);

        assertThat(playerElement, is(visible()));
    }
}
