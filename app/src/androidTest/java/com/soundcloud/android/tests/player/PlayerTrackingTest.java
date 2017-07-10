package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerTrackingTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_EXPAND_COLLAPSE = "specs/player-expand-collapse.spec";
    private static final String TEST_SCENARIO_PAUSE_PLAY_FROM_MINIPLAYER = "specs/player-mini-play-pause.spec";
    private static final String TEST_SCENARIO_PAUSE_PLAY_FROM_FULLPLAYER = "specs/player-full-play-pause.spec";
    private static final String CLICK_ARTIST_NAVIGATION = "specs/player-artist-navigation.spec";

    private StreamScreen streamScreen;

    public PlayerTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playerUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        streamScreen = new StreamScreen(solo);
    }

    public void testTrackPlayerExpandAndCollapse() throws Exception {
        mrLocalLocal.startEventTracking();

        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        player.pressCloseButton();
        player.waitForCollapsedPlayer();

        mrLocalLocal.verify(TEST_SCENARIO_EXPAND_COLLAPSE);
    }

    public void testTrackMiniPlayerPauseAndPlay() throws Exception {

        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        player.pressCloseButton();
        player.waitForCollapsedPlayer();

        mrLocalLocal.startEventTracking();

        player.toggleFooterPlay(); //we are stopping the playback
        player.toggleFooterPlay(); //we are starting again the playback

        mrLocalLocal.verify(TEST_SCENARIO_PAUSE_PLAY_FROM_MINIPLAYER);
    }

    public void testTrackPlayerPauseAndPlay() throws Exception {

        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        mrLocalLocal.startEventTracking();

        player.clickArtwork() //we are stopping the playback
              .tapPlayButton(); //we are starting again the playback

        mrLocalLocal.verify(TEST_SCENARIO_PAUSE_PLAY_FROM_FULLPLAYER);
    }

    public void testTrackClickingArtistName() throws Exception {
        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        mrLocalLocal.startEventTracking();

        player.clickCreator();

        mrLocalLocal.verify(CLICK_ARTIST_NAVIGATION);
    }
}
