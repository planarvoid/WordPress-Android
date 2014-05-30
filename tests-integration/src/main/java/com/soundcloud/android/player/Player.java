package com.soundcloud.android.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.elements.PlayerElement;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

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

    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() throws Exception {
        assertThat(playerElement.isVisible(), is(false));
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() throws Exception {
        playFirstTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenCloseButtonIsPressed() throws Exception {
        playFirstTrack();
        playerElement.pressCloseButton();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenSwipingDown() throws Exception {
        playFirstTrack();
        solo.swipeDown();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerExpandsOnFooterTap() throws Exception {
        playFirstTrack();
        playerElement.pressBackToCollapse();
        playerElement.tapFooter();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() throws Exception {
        playFirstTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isFooterInPlayingState(), is(true));
        playerElement.toggleFooterPlay();
        assertThat(playerElement.isFooterInPlayingState(), is(false));
    }

    public void testPlayStateCanBeToggledFromFullPlayer() throws Exception {
        playFirstTrack();
        assertThat(playerElement.isPlayControlsVisible(), is(false));
        playerElement.togglePlay();
        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    public void testPlayerIsExpandedAfterClickingTrack() throws Exception {
        playFirstTrack();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testSkippingWithNextAndPreviousChangesTrack() throws Exception {
        playFirstTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.togglePlay();

        playerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSkippingWithNextAndPreviousChangesTrackWhilePlaying() throws Exception {
        playFirstTrack();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.tapTrackPageNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapTrackPagePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSwipingNextAndPreviousChangesTrack() throws Exception {
        playFirstTrack();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));    }

    public void testPlayerRemainsPausedWhenSkipping() throws Exception {
        playFirstTrack();

        playerElement.togglePlay();
        playerElement.tapNext();

        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    public void testPreviousButtonDoesNothingOnFirstTrack() throws Exception {
        playFirstTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.togglePlay();

        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testNextButtonDoesNothingOnLastTrack() {
        playSingleTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.togglePlay();

        playerElement.tapNext();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    private void playFirstTrack() throws InterruptedException {
        StreamScreen streamScreen = new StreamScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        solo.sleep(2000); // TODO: Tempory solution to work around bug where play queue is never set (Bug #1690)
        streamScreen.clickFirstTrack();
        waiter.waitForExpandedPlayer();
        playerElement.waitForContent();
    }

    private void playSingleTrack() {
        ExploreScreen exploreScreen = openExploreFromMenu();
        exploreScreen.touchTrendingAudioTab();
        waiter.waitForContentAndRetryIfLoadingFailed();
        exploreScreen.playFirstTrack();
        waiter.waitForExpandedPlayer();
        playerElement.waitForContent();
    }

    private ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }

}
