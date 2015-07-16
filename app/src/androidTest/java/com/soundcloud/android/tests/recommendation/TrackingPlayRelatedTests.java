package com.soundcloud.android.tests.recommendation;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class TrackingPlayRelatedTests extends TrackingActivityTest<MainActivity> {

    public static final String PLAY_RELATED_LIKES_AND_TRIGGER_MANUAL = "play_related_from_playlist";

    public TrackingPlayRelatedTests() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Flag.PLAY_RELATED_TRACKS);
        super.setUp();
    }

    public void testTrackingFromLikesWithTriggerManualAndNextTrackPlaysWithTriggerAuto() throws Exception {
        final VisualPlayerElement player = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylistAt(0)
                .clickFirstTrackOverflowButton()
                .clickStartRadio();

        assertTrue(player.isExpandedPlayerPlaying());
        player.swipePrevious();
        player.waitForTheExpandedPlayerToPlayNextTrack();

        verifier.assertScenario(PLAY_RELATED_LIKES_AND_TRIGGER_MANUAL);
    }

}
