package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;
import com.soundcloud.android.tests.helpers.PlayerHelper;

public class Player extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;
    private StreamScreen streamScreen;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();
        playerElement = null;
        streamScreen = new StreamScreen(solo);
    }

    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() throws Exception {
        playerElement = new VisualPlayerElement(solo);
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

    public void testPlayerExpandsOnFooterTap() throws Exception {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        playerElement.tapFooter();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testPlayerCanBeStartedFromProfiles() {
        menuScreen.open()
                .clickUserProfile()
                .playTrack(0);

        assertThat(new VisualPlayerElement(solo), is(Visible()));
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
        playerElement = streamScreen.clickFirstTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.clickArtwork();

        playerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSkippingWithNextAndPreviousChangesTrackWhilePlaying() throws Exception {
        playerElement = streamScreen.clickFirstTrack();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.tapTrackPageNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapTrackPagePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSwipingNextAndPreviousChangesTrack() throws Exception {
        playTrackFromLikes();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

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

    public void testUserButtonGoesToUserProfile() {
        playSingleTrack();
        String originalUser = playerElement.getTrackCreator();
        playerElement.clickCreator();

        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen.getUserName(), is(equalTo(originalUser)));
    }

    private void playExploreTrack() {
        final StreamScreen streamScreen = new StreamScreen(solo);
        playerElement = PlayerHelper.openPlayer(this, NavigationHelper.openExploreFromMenu(streamScreen));
    }

    private void playSingleTrack() {
        final ExploreScreen exploreScreen = menuScreen.open().clickExplore();
        exploreScreen.touchTrendingAudioTab();
        exploreScreen.playFirstTrack();
        playerElement = new VisualPlayerElement(solo);
    }

    private void playTrackFromLikes() {
        playerElement = menuScreen.open().clickLikes().clickItem(1);
        waiter.waitForExpandedPlayer();
    }
}
