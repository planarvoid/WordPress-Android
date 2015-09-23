package com.soundcloud.android.tests.likes;


import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SongsCanBePlayedInRandomOrderTest extends ActivityTest<MainActivity> {

    private VisualPlayerElement playerElement;

    @Override
    protected void logInHelper() {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
    }

    public SongsCanBePlayedInRandomOrderTest() {
        super(MainActivity.class);
    }


    public void testSongIsPlayedWhenShuffleEnabled() {
        playerElement = menuScreen.open()
                .clickLikes()
                .clickShuffleButton();
        assertThat(playerElement, is(playing()));
    }

}
