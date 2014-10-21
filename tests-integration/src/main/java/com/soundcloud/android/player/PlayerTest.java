package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;
import com.soundcloud.android.tests.helpers.PlayerHelper;
import com.soundcloud.android.tests.with.With;

public class PlayerTest extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();
        playerElement = null;
        streamScreen = new StreamScreen(solo);
    }

    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() {
        playerElement = new VisualPlayerElement(solo);
        assertThat(playerElement.isVisible(), is(false));
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenCloseButtonIsPressed() {
        playExploreTrack();
        playerElement.pressCloseButton();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenSwipingDown() {
        playExploreTrack();
        solo.swipeDown();
        assertThat(playerElement.isCollapsed(), is(true));
    }

    public void testPlayerExpandsOnFooterTap() {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        playerElement.tapFooter();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testPlayerCanBeStartedFromProfiles() {
        menuScreen.open()
                .clickUserProfile()
                .playTrack(0);

        assertThat(new VisualPlayerElement(solo), is(visible()));
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() {
        playExploreTrack();
        playerElement.pressBackToCollapse();
        assertThat(playerElement.isFooterInPlayingState(), is(true));
        playerElement.toggleFooterPlay();
        assertThat(playerElement.isFooterInPlayingState(), is(false));
    }

    public void testPlayStateCanBeToggledFromFullPlayer() {
        playExploreTrack();
        assertThat(playerElement.isPlayControlsVisible(), is(false));
        playerElement.clickArtwork();
        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    public void testPlayerIsExpandedAfterClickingTrack() {
        playExploreTrack();
        assertThat(playerElement.isExpanded(), is(true));
    }

    public void testSkippingWithNextAndPreviousChangesTrack() {
        playerElement = streamScreen.clickFirstTrack();
        String originalTrack = playerElement.getTrackTitle();
        playerElement.clickArtwork();

        playerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testSwipingNextAndPreviousChangesTrack() {
        playTrackFromLikes();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
        playerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(playerElement.getTrackTitle())));
    }

    public void testPlayerRemainsPausedWhenSkipping() {
        playExploreTrack();

        playerElement.clickArtwork();
        playerElement.tapNext();

        assertThat(playerElement.isPlayControlsVisible(), is(true));
    }

    private void playLastTrackOnPlaylist() {
        PlaylistDetailsScreen playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("Two Tracks Playlist"));
        solo.scrollToBottom(solo.getCurrentListView());
        playerElement = playlistDetailsScreen.clickSecondTrack();
        playerElement.waitForExpandedPlayer();
    }

    public void testUserButtonGoesToUserProfile() {
        playSingleTrack();
        String originalUser = playerElement.getTrackCreator();
        ProfileScreen profileScreen = playerElement.clickCreator();

        assertThat(playerElement, is(collapsed()));
        assertThat(profileScreen.getUserName(), is(equalTo(originalUser)));
    }

    public void testPlayerShowTheTrackDesciption() throws Exception {
        playExploreTrack();

        String originalTitle = playerElement.getTrackTitle();
        playerElement.clickMenu().info().click();

        final TrackInfoScreen trackInfoScreen = new TrackInfoScreen(solo);
        assertTrue(trackInfoScreen.waitForDialog());
        assertThat(trackInfoScreen.getTitle(), is(equalTo(originalTitle)));
    }

    public void testPlayerTrackInfoLinksToComments() throws Exception {
        playTrackFromLikes();

        String originalTitle = playerElement.getTrackTitle();
        playerElement.clickMenu().info().click();

        final TrackInfoScreen trackInfoScreen = new TrackInfoScreen(solo);
        assertTrue(trackInfoScreen.waitForDialog());

        TrackCommentsScreen trackCommentsScreen = trackInfoScreen.clickComments();
        assertThat(originalTitle, is(equalTo((trackCommentsScreen.getTitle()))));
    }

    public void testPlayerTrackMakeComment() throws Exception {
        playTrackFromLikes();

        String originalTitle = playerElement.getTrackTitle();
        playerElement.clickMenu().comment().click();

        final AddCommentScreen addCommentScreen = new AddCommentScreen(solo);
        assertTrue(addCommentScreen.waitForDialog());
        assertTrue(addCommentScreen.getTitle().contains(originalTitle));
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
        playerElement.waitForExpandedPlayer();
    }
}
