package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackLikesTest extends ActivityTest<MainActivity> {
    private LikesScreen screen;

    public TrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        menuScreen = new MenuScreen(solo);
        screen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testClickingShuffleButtonOpensPlayer() {
        VisualPlayerElement playerElement = screen.clickShuffleButton();

        assertThat(playerElement, is(visible()));
    }
}
