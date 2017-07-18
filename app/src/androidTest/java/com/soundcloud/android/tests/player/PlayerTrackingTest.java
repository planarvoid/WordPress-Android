package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerTrackingTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_PAUSE_PLAY_FROM_MINIPLAYER = "specs/player-mini-play-pause.spec";
    private static final String TEST_SCENARIO_PAUSE_PLAY_FROM_FULLPLAYER = "specs/player-full-play-pause.spec";
    private static final String TEST_SCENARIO_EXPAND_COLLAPSE = "specs/player-click-expand-collapse.spec";
    private static final String TEST_SCENARIO_MANUAL_EXPAND = "specs/player-click-manual-expand.spec";
    private static final String TEST_SCENARIO_MANUAL_SWIPE = "specs/player-swipe-manual.spec";
    private static final String TEST_SCENARIO_SCRUB_FORWARD_AND_BACKWARD = "specs/player-scrub-forward-backward.spec";
    private static final String TEST_SCENARIO_CLICK_ARTIST_NAVIGATION = "specs/player-artist-navigation.spec";

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

    private VisualPlayerElement setUpMiniplayer() {
        // miniplayer starts hidden so let's play something so it appears and then immediately collapse it
        VisualPlayerElement playerElement = streamScreen.clickFirstRepostedTrack().waitForExpandedPlayer();
        playerElement.clickArtwork();
        playerElement.pressCloseButton();
        playerElement.waitForCollapsedPlayer();
        return playerElement;
    }

    public void testClickingATrackIsTrackedAsAutoExpansionOfThePlayerAndManualCollapseWhenClickingTheCloseButton() throws Exception {
        mrLocalLocal.startEventTracking();

        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        player.pressCloseButton();
        player.waitForCollapsedPlayer();

        mrLocalLocal.verify(TEST_SCENARIO_EXPAND_COLLAPSE);
    }

    public void testTrackMiniPlayerPauseAndPlay() throws Exception {
        final VisualPlayerElement player = setUpMiniplayer();

        mrLocalLocal.startEventTracking();

        player.toggleFooterPlay();
        player.toggleFooterPlay();

        mrLocalLocal.verify(TEST_SCENARIO_PAUSE_PLAY_FROM_MINIPLAYER);
    }

    public void testTrackPlayerPauseAndPlay() throws Exception {
        final VisualPlayerElement expandedPlayer = streamScreen.clickFirstRepostedTrack()
                                                               .waitForExpandedPlayer();

        mrLocalLocal.startEventTracking();

        expandedPlayer.clickArtwork() //we are stopping the playback
                      .tapPlayButton(); //we are starting again the playback

        mrLocalLocal.verify(TEST_SCENARIO_PAUSE_PLAY_FROM_FULLPLAYER);
    }

    public void testClickingMiniplayerExpandsItAndTriggerIsTrackedAsManual() throws Exception {
        setUpMiniplayer();
        mrLocalLocal.startEventTracking();

        streamScreen.clickMiniplayer();

        mrLocalLocal.verify(TEST_SCENARIO_MANUAL_EXPAND);
    }

    public void testSwipingMiniplayerAndTriggerIsTrackedAsManual() throws Exception {
        setUpMiniplayer();
        mrLocalLocal.startEventTracking();

        streamScreen.swipeUpMiniplayer();
        streamScreen.swipeDownMiniplayer();

        mrLocalLocal.verify(TEST_SCENARIO_MANUAL_SWIPE);
    }

    public void testTrackPlayerScrub() throws Exception {
        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        mrLocalLocal.startEventTracking();

        player.scrubForward();
        player.scrubBackward();

        mrLocalLocal.verify(TEST_SCENARIO_SCRUB_FORWARD_AND_BACKWARD);
    }

    public void testTrackClickingArtistName() throws Exception {
        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        mrLocalLocal.startEventTracking();

        player.clickCreator();

        mrLocalLocal.verify(TEST_SCENARIO_CLICK_ARTIST_NAVIGATION);
    }
}
