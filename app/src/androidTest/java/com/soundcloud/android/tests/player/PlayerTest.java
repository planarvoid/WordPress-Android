package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.framework.helpers.PlayerHelper.playPublicTrack;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlayerTest extends ActivityTest<MainActivity> {

    private VisualPlayerElement visualPlayerElement;
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        visualPlayerElement = null;
        streamScreen = new StreamScreen(solo);
    }

    @Test
    public void testPlayerShouldNotBeVisibleWhenPlayQueueIsEmpty() throws Exception {
        visualPlayerElement = new VisualPlayerElement(solo);
        assertThat(visualPlayerElement.isVisible(), is(false));
    }

    @Test
    public void testPlayerCollapsesWhenBackButtonIsPressed() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        visualPlayerElement.pressBackToCollapse();
        assertThat(visualPlayerElement.isCollapsed(), is(true));
    }

    @Test
    public void testPlayerCollapsesWhenCloseButtonIsPressed() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        visualPlayerElement.pressCloseButton();
        assertThat(visualPlayerElement.isCollapsed(), is(true));
    }

    @Test
    public void testPlayerExpandsOnFooterTap() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        visualPlayerElement.pressBackToCollapse();
        visualPlayerElement.tapFooter();
        assertThat(visualPlayerElement.isExpanded(), is(true));
    }

    @Test
    public void testPlayStateCanBeToggledFromPlayerFooter() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        visualPlayerElement.pressBackToCollapse();
        assertThat(visualPlayerElement, is(collapsed()));
        assertThat(visualPlayerElement, is(playing()));

        visualPlayerElement.toggleFooterPlay();
        assertThat(visualPlayerElement, is(not(playing())));
    }

    @Test
    public void testPlayStateCanBeToggledFromFullPlayer() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        assertThat(visualPlayerElement, is(playing()));
        visualPlayerElement.clickArtwork();
        assertThat(visualPlayerElement, is(not(playing())));
    }

    @Test
    public void testPlayerIsExpandedAfterClickingTrack() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        assertThat(visualPlayerElement.isExpanded(), is(true));
    }

    @Test
    public void testSkippingWithNextAndPreviousChangesTrack() throws Exception {
        visualPlayerElement = streamScreen.clickFirstTrackCard();
        String originalTrack = visualPlayerElement.getTrackTitle();
        visualPlayerElement.clickArtwork();

        visualPlayerElement.tapNext();
        assertThat(originalTrack, is(not(equalTo(visualPlayerElement.getTrackTitle()))));
        visualPlayerElement.tapPrevious();
        assertThat(originalTrack, is(equalTo(visualPlayerElement.getTrackTitle())));
    }

    @Test
    public void testSwipingNextAndPreviousChangesTrack() throws Exception {
        playTrackFromStream();
        String originalTrack = visualPlayerElement.getTrackTitle();

        visualPlayerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(visualPlayerElement.getTrackTitle()))));
        visualPlayerElement.swipePrevious();
        assertThat(originalTrack, is(equalTo(visualPlayerElement.getTrackTitle())));
    }

    @Test
    public void testPlayerRemainsPausedWhenSkipping() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);

        visualPlayerElement.clickArtwork();
        visualPlayerElement.tapNext();

        assertThat(visualPlayerElement, is(not(playing())));
    }

    @Test
    public void testUserButtonGoesToUserProfile() throws Exception {
        visualPlayerElement = playPublicTrack(this, mainNavHelper);
        String originalUser = visualPlayerElement.getTrackCreator();

        ProfileScreen profileScreen = visualPlayerElement.clickCreator();

        assertThat(visualPlayerElement, is(collapsed()));
        assertThat(profileScreen.getUserName(), is(equalTo(originalUser)));
    }

    private void playTrackFromStream() {
        visualPlayerElement = streamScreen.clickFirstTrackCard();
    }
}
