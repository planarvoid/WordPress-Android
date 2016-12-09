package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.Matchers;

import android.content.Context;

public class PlaylistItemsTest extends ActivityTest<MainActivity> {

    private String playlist;
    private Context context;
    private final OfflineContentHelper offlineContentHelper;

    public PlaylistItemsTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.addToPlaylistUser;
    }

    @Override
    protected void setUp() throws Exception {
        context = getInstrumentation().getTargetContext();
        super.setUp();
        playlist = String.valueOf(System.currentTimeMillis());
    }

    public void testAddTrackToPlaylistFromStream() {
        StreamScreen streamScreen = new StreamScreen(solo);
        StreamCardElement firstTrack = streamScreen.scrollToFirstTrack();
        String trackAddedTitle = firstTrack.trackTitle();

        firstTrack
                .clickOverflowButton()
                .clickAddToPlaylist()
                .clickCreateNewPlaylist()
                .enterTitle(playlist)
                .clickDoneAndReturnToStream();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    public void testAddTrackToPlaylistFromPlayer() {
        enableOfflineContent(context);

        final TrackLikesScreen trackLikesScreen = mainNavHelper
                .goToCollections()
                .clickLikedTracksPreview();

        assertThat(trackLikesScreen, is(visible()));

        final String trackAddedTitle = trackLikesScreen.getTrackTitle(0);

        CreatePlaylistScreen createPlaylistScreen = trackLikesScreen.clickTrack(0)
                                                                    .clickMenu()
                                                                    .clickAddToPlaylist()
                                                                    .clickCreateNewPlaylist();

        assertThat(createPlaylistScreen, Matchers.is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isOnScreen(), Matchers.is(true));

        createPlaylistScreen.enterTitle(playlist)
                            .clickDoneAndReturnToPlayer()
                            .pressBackToCollapse();

        trackLikesScreen.goBack();

        assertPlaylistContainsTrack(trackAddedTitle);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ConfigurationHelper.disableOfflineContent(context);
        offlineContentHelper.clearOfflineContent(context);
    }

    private void assertPlaylistContainsTrack(String trackTitle) {
        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        collectionScreen.pullToRefresh();
        PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.clickPlaylistsPreview()
                                                                      .scrollToAndClickPlaylistWithTitle(playlist);

        assertThat(playlistDetailsScreen.getTitle(), is(playlist));
        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(true));
    }
}
