package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDetailsTest extends ActivityTest<LauncherActivity> {
    private static final String TEST_PLAYLIST_SHUFFLE = "specs/playlist-shuffle-events.spec";

    private PlaylistsScreen playlistsScreen;

    public PlaylistDetailsTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playlistUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        getExperiments().set(PlaylistAndAlbumsPreviewsExperiment.CONFIGURATION, PlaylistAndAlbumsPreviewsExperiment.VARIANT_CONTROL);

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistsScreen = collectionScreen.clickPlaylistsPreview();
    }

    public void testHeaderPlayClickShouldOpenPlayer() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        assertThat(player, is(not(visible())));
        final VisualPlayerElement visualPlayer = playlistsScreen.clickOnFirstPlaylist()
                                                                .clickHeaderPlay()
                                                                .waitForExpandedPlayer();
        assertThat(visualPlayer, is(expanded()));
    }

    public void testShufflePlaylist() throws Exception {
        mrLocalLocal.startEventTracking();

        VisualPlayerElement player = new VisualPlayerElement(solo);
        PlaylistOverflowMenu overflowMenu = playlistsScreen.clickOnFirstPlaylist()
                                                           .clickPlaylistOverflowButton();

        assertThat(player, is(not(visible())));
        overflowMenu.shuffle();
        assertThat(player, is(visible()));

        mrLocalLocal.verify(TEST_PLAYLIST_SHUFFLE);
    }

}
