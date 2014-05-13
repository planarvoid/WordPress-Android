package com.soundcloud.android.player;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.elements.PlayerElement;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

import android.test.suitebuilder.annotation.Suppress;

public class Player extends ActivityTestCase<MainActivity> {

    private PlayerElement playerElement;

    public Player() {
        super(MainActivity.class);
        setDependsOn(Feature.VISUAL_PLAYER);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        playerElement = new PlayerElement(solo);
    }

    public void testPlayerExpandsOnFooterTap() {
        playerElement.tapFooter();
        assertEquals("Player should be expanded", true, playerElement.isExpanded());
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() {
        playerElement.tapFooter();
        solo.goBack();
        assertEquals("Player should be collapsed", true, playerElement.isCollapsed());
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() {
        assertEquals("Footer should show paused state", false, playerElement.isFooterInPlayingState());
        playerElement.toggleFooterPlay();
        assertEquals("Footer should show playing state", true, playerElement.isFooterInPlayingState());
    }

    public void testPlayerIsExpandedAfterClickingTrack() {
        playFirstTrack();
        assertEquals("Player should be expanded", true, playerElement.isExpanded());
    }

    public void testSkippingWithNextAndPreviousChangesTrack() {
        playFirstTrack();
        String originalTrack = playerElement.getFooterTitle();

        playerElement.tapNext();
        assertEquals("Track should be different", false, originalTrack.equals(playerElement.getFooterTitle()));

        playerElement.tapPrevious();
        assertEquals("Track should be the same", true, originalTrack.equals(playerElement.getFooterTitle()));
    }

    @Suppress
    public void testPlayerRemainsPausedWhenSkipping() {
        playFirstTrack();

        playerElement.toggleFooterPlay();
        playerElement.tapNext();

        assertEquals("Footer should show paused state", false, playerElement.isFooterInPlayingState());
    }

    public void testPreviousButtonDoesNothingOnFirstTrack() {
        playFirstTrack();
        String originalTrack = playerElement.getFooterTitle();

        playerElement.tapPrevious();
        assertEquals("Track should be the same", true, originalTrack.equals(playerElement.getFooterTitle()));
    }

    public void testNextButtonDoesNothingOnLastTrack() {
        playSingleTrack();
        String originalTrack = playerElement.getFooterTitle();

        playerElement.tapNext();
        assertEquals("Track should be the same", true, originalTrack.equals(playerElement.getFooterTitle()));
    }

    private void playFirstTrack() {
        StreamScreen streamScreen = new StreamScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen.clickFirstTrack();
    }

    private void playSingleTrack() {
        ExploreScreen exploreScreen = openExploreFromMenu();
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        exploreScreen.playFirstTrack();
        waiter.waitForExpandedPlayer();
    }

    private ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }

}
