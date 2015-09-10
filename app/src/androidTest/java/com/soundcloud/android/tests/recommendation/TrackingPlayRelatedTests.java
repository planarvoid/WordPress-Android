package com.soundcloud.android.tests.recommendation;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
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
        setRequiredEnabledFeatures(Flag.EVENTLOGGER_AUDIO_V1);
        super.setUp();
    }

    public void testTrackingFromLikesWithTriggerManualAndNextTrackPlaysWithTriggerAuto() throws Exception {
        startEventTracking();

        final VisualPlayerElement player = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylist(With.text("Trigger Auto Test Playlist"))
                .clickFirstTrackOverflowButton()
                .clickPlayRelatedTracks();

        assertTrue(player.isExpandedPlayerPlaying());
        player.swipePrevious();
        player.waitForTheExpandedPlayerToPlayNextTrack();

        finishEventTracking(PLAY_RELATED_LIKES_AND_TRIGGER_MANUAL);
    }

}
