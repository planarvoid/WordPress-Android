package com.soundcloud.android.tests.promoted;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
public class PromotedStreamTrackingTest extends TrackingActivityTest<MainActivity> {

    private static final String PROMOTED_PLAY = "promoted-play";

    public PromotedStreamTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    // TODO: https://github.com/soundcloud/SoundCloud-Android/issues/3202
    public void ignoreTestPlayPromotedTrackFromStream() {
        startEventTracking(PROMOTED_PLAY);
        StreamScreen streamScreen = menuScreen
                .open()
                .clickStream();

        assertThat(streamScreen.isFirstTrackPromoted(), is(true));

        VisualPlayerElement playerElement = streamScreen.clickFirstTrack();
        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        finishEventTracking();
    }

}
