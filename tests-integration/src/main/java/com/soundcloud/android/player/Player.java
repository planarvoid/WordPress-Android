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
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();
        playerElement = null;
    }

    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() throws Exception {
        playerElement = new PlayerElement(solo);
        assertThat(playerElement.isVisible(), is(false));
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() throws Exception {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenCloseButtonIsPressed() throws Exception {
        playExploreTrack();
        playerElement.pressCloseButton();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenSwipingDown() throws Exception {
        playExploreTrack();
        solo.swipeDown();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void ignoretestPlayerExpandsOnFooterTap() throws Exception {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        playerElement.tapFooter();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() throws Exception {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isFooterInPlayingState(), is(true));
        playerElement.toggleFooterPlay();
        assertThat(playerElement.isFooterInPlayingState(), is(false));
    }

    public void testPlayStateCanBeToggledFromFullPlayer() throws Exception {
        playExploreTrack();
        assertThat(playerElement.isPlayControlsVisible(), is(false));
        playerElement.clickArtwork();
        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    public void testPlayerIsExpandedAfterClickingTrack() throws Exception {
        playExploreTrack();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testSkippingWithNextAndPreviousChangesTrack() throws Exception {
        playExploreTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.clickArtwork();

        playerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSkippingWithNextAndPreviousChangesTrackWhilePlaying() throws Exception {
        playExploreTrack();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.tapTrackPageNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapTrackPagePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSwipingNextAndPreviousChangesTrack() throws Exception {
        playExploreTrack();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));    }

    public void testPlayerRemainsPausedWhenSkipping() throws Exception {
        playExploreTrack();

        playerElement.clickArtwork();
        playerElement.tapNext();

        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    public void testPreviousButtonDoesNothingOnFirstTrack() throws Exception {
        playExploreTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.clickArtwork();

        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testNextButtonDoesNothingOnLastTrack() {
        playSingleTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.clickArtwork();

        playerElement.tapNext();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    private void playExploreTrack() {
        ExploreScreen exploreScreen = openExploreFromMenu();
        waiter.waitForContentAndRetryIfLoadingFailed();
        exploreScreen.playFirstTrack();
        playerElement = new PlayerElement(solo);
        waiter.waitForExpandedPlayer();
        playerElement.waitForContent();
    }

    private void playSingleTrack(){
        menuScreen.open().clickLikes().clickItem(2);
        playerElement = new PlayerElement(solo);
    }

    private ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }

}
