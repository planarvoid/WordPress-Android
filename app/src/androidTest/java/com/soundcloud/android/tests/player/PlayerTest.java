package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.PlayerHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerTest extends ActivityTest<MainActivity> {

    private VisualPlayerElement visualPlayerElement;
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        visualPlayerElement = null;
        streamScreen = new StreamScreen(solo);
    }

    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() {
        visualPlayerElement = new VisualPlayerElement(solo);
        assertThat(visualPlayerElement.isVisible(), is(false));
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() {
        playExploreTrack();
        visualPlayerElement.pressBackToCollapse();
        assertThat(visualPlayerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenCloseButtonIsPressed() {
        playExploreTrack();
        visualPlayerElement.pressCloseButton();
        assertThat(visualPlayerElement.isCollapsed(), is(true));
    }

    public void testPlayerCollapsesWhenSwipingDown() {
        playExploreTrack();
        solo.swipeDown();
        assertThat(visualPlayerElement.isCollapsed(), is(true));
    }

    public void testPlayerExpandsOnFooterTap() {
        playExploreTrack();
        visualPlayerElement.pressBackToCollapse();
        visualPlayerElement.tapFooter();
        assertThat(visualPlayerElement.isExpanded(), is(true));
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() {
        playExploreTrack();
        visualPlayerElement.pressBackToCollapse();
        assertThat(visualPlayerElement, is(collapsed()));
        assertThat(visualPlayerElement, is(playing()));

        visualPlayerElement.toggleFooterPlay();
        assertThat(visualPlayerElement, is(not(playing())));
    }

    public void testPlayStateCanBeToggledFromFullPlayer() {
        playExploreTrack();
        assertThat(visualPlayerElement, is(playing()));
        visualPlayerElement.clickArtwork();
        assertThat(visualPlayerElement, is(not(playing())));
    }

    public void testPlayerIsExpandedAfterClickingTrack() {
        playExploreTrack();
        assertThat(visualPlayerElement.isExpanded(), is(true));
    }

    public void testSkippingWithNextAndPreviousChangesTrack() {
        visualPlayerElement = streamScreen.clickFirstTrackCard();
        String originalTrack = visualPlayerElement.getTrackTitle();
        visualPlayerElement.clickArtwork();

        visualPlayerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(visualPlayerElement.getTrackTitle()))));
        visualPlayerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(visualPlayerElement.getTrackTitle())));
    }

    public void testSwipingNextAndPreviousChangesTrack() {
        playTrackFromStream();
        String originalTrack = visualPlayerElement.getTrackTitle();

        visualPlayerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(visualPlayerElement.getTrackTitle()))));
        visualPlayerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(visualPlayerElement.getTrackTitle())));
    }

    public void testPlayerRemainsPausedWhenSkipping() {
        playExploreTrack();

        visualPlayerElement.clickArtwork();
        visualPlayerElement.tapNext();

        assertThat(visualPlayerElement, is(not(playing())));
    }

    public void testUserButtonGoesToUserProfile() {
        playSingleTrack();
        String originalUser = visualPlayerElement.getTrackCreator();
        ProfileScreen profileScreen = visualPlayerElement.clickCreator();

        assertThat(visualPlayerElement, is(collapsed()));
        assertThat(profileScreen.getUserName(), is(equalTo(originalUser)));
    }

    public void testPlayerShowTheTrackDescription() {
        visualPlayerElement = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("zzz track with description")
                .clickFirstTrackItem();

        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackInfoScreen trackInfoScreen = visualPlayerElement
                .clickMenu()
                .clickInfo();

        assertThat(originalTitle, is(equalTo(trackInfoScreen.getTitle())));
        assertTrue(trackInfoScreen.getDescription().isVisible());
    }

    public void testPlayerShowTheTrackNoDescription() {
        visualPlayerElement = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("aaazzz track with no description")
                .clickFirstTrackItem();

        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackInfoScreen trackInfoScreen = visualPlayerElement
                .clickMenu()
                .clickInfo();

        assertThat(originalTitle, is(equalTo(trackInfoScreen.getTitle())));
        assertTrue(trackInfoScreen.getNoDescription().isVisible());
    }

    public void testListOfCommentsCanBePaged() {
        visualPlayerElement = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("lots o' comments")
                .clickFirstTrackItem();

        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackCommentsScreen trackCommentsScreen = visualPlayerElement
                .clickMenu()
                .clickInfo()
                .clickComments();

        assertThat(originalTitle, is(equalTo((trackCommentsScreen.getTitle()))));

        int initialCommentsCount = trackCommentsScreen.getCommentsCount();
        trackCommentsScreen.scrollToBottomOfComments();

        int nextCommentsCount = trackCommentsScreen.getCommentsCount();
        assertThat(nextCommentsCount, is(greaterThan(initialCommentsCount)));
    }

    public void testPlayerTrackMakeComment() {
        playTrackFromStream();

        final AddCommentScreen addCommentScreen = visualPlayerElement
                .clickMenu()
                .clickComment();

        assertTrue(addCommentScreen.waitForDialog());
    }

    public void testShouldHideCommentingWhenTrackHasBlockedComments() {
        visualPlayerElement = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("zzzz yowz no comments")
                .clickFirstTrackItem();

        assertThat(visualPlayerElement.clickMenu().commentItem(), is(not(visible())));
    }

    private void playExploreTrack() {
        visualPlayerElement = PlayerHelper.openPlayer(this, mainNavHelper.goToExplore());
    }

    private void playSingleTrack() {
        final ExploreScreen exploreScreen = mainNavHelper.goToExplore();
        exploreScreen.touchTrendingAudioTab();
        exploreScreen.playFirstTrack();
        visualPlayerElement = new VisualPlayerElement(solo);
    }

    private void playTrackFromStream() {
        visualPlayerElement = streamScreen.clickFirstTrackCard();
    }
}
