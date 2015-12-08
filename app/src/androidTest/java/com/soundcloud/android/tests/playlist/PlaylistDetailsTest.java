package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public class PlaylistDetailsTest extends ActivityTest<ResolveActivity> {
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetailsTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData( Uri.parse("soundcloud:playlists:116114846")));
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
    }

    public void testPlaylistDetailsScreenShouldBeVisibleOnPlaylistClick() {
        assertThat(playlistDetailsScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    public void testHeaderPlayClickShouldNotOpenPlayer() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        assertThat(player, is(not(visible())));
        playlistDetailsScreen.clickHeaderPlay();
        assertThat(player, is(visible()));

        playlistDetailsScreen.clickHeaderPause();
        assertThat(playlistDetailsScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    public void testToggleStateIsNotCheckedAfterPausingPlayer() {
        playlistDetailsScreen.clickHeaderPlay();
        playlistDetailsScreen.clickHeaderPause();

        assertThat(playlistDetailsScreen.isPlayToggleChecked(), is(false));
    }

    public void disabled_testRemovingAndAddingTrackFromPlaylist() throws Exception {
        String title = playlistDetailsScreen.getTitle();
        int initialTrackCount = playlistDetailsScreen.getTrackCount();

        VisualPlayerElement player = playlistDetailsScreen.clickFirstTrack();
        assertTrue("Player did not expand", player.waitForExpandedPlayer().isExpanded());
        player.pressBackToCollapse();

        TrackItemMenuElement menu = playlistDetailsScreen.clickFirstTrackOverflowButton();
        menu.clickRemoveFromPlaylist();

        assertThat(playlistDetailsScreen.getTrackCount(), is(initialTrackCount - 1));

        player.tapFooter();
        AddToPlaylistScreen addToPlaylistScreen = player.clickMenu().clickAddToPlaylist();
        addToPlaylistScreen.clickPlaylistWithTitleFromPlayer(title);
        player.pressBackToCollapse();

        assertThat(playlistDetailsScreen.getTrackCount(), is(initialTrackCount));
    }
}
