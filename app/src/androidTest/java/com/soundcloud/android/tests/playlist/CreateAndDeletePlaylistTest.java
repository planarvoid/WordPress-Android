package com.soundcloud.android.tests.playlist;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.Matchers;

import android.content.Context;

public class CreateAndDeletePlaylistTest extends ActivityTest<MainActivity> {

    private String playlist;
    private Context context;
    private final OfflineContentHelper offlineContentHelper;

    public CreateAndDeletePlaylistTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.createAndDeletePlaylistUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        getExperiments().set(PlaylistAndAlbumsPreviewsExperiment.CONFIGURATION, PlaylistAndAlbumsPreviewsExperiment.VARIANT_CONTROL);

        context = getInstrumentation().getTargetContext();
        playlist = String.valueOf(System.currentTimeMillis());
    }

    @Override
    protected void addInitialStubMappings() {
        // Fix : test can't find a track in the stream because the account fills up with playlists.
        //
        // This can happen because while the creation is synced, the deletion may not be synced (if the test runner kills the app before),
        // so just prevent the creation from syncing :)
        stubFor(post(urlPathEqualTo("/playlists")).willReturn(aResponse().withStatus(500)));
    }

    public void testCreatePlaylistFromStreamAndDeleteFromPlaylistDetailsScreen() throws Exception {
        StreamScreen streamScreen = new StreamScreen(solo);
        StreamCardElement firstTrack = streamScreen.scrollToFirstTrack();
        String trackAddedTitle = firstTrack.trackTitle();

        firstTrack.clickOverflowButton()
                  .clickAddToPlaylist()
                  .clickCreateNewPlaylist()
                  .enterTitle(playlist)
                  .clickDoneAndReturnToStream();

        final PlaylistDetailsScreen playlistDetailsScreen = assertPlaylistContainsTrack(trackAddedTitle);

        final PlaylistsScreen playlistsScreen = playlistDetailsScreen.clickPlaylistOverflowButton()
                                                                     .clickDelete()
                                                                     .clickConfirm();

        assertThat(playlistsScreen.getPlaylistWithTitle(playlist).isOnScreen(), Matchers.is(false));
    }

    public void testCreatePlaylistFromPlayerAndDeleteFromPlaylistsScreen() throws Exception {
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

        final PlaylistDetailsScreen playlistDetailsScreen = assertPlaylistContainsTrack(trackAddedTitle);

        final PlaylistsScreen playlistsScreen = playlistDetailsScreen.goBackToPlaylists()
                                                                     .scrollToPlaylistWithTitle(playlist)
                                                                     .clickOverflow()
                                                                     .clickDelete()
                                                                     .clickConfirm();

        assertThat(playlistsScreen.getPlaylistWithTitle(playlist).isOnScreen(), Matchers.is(false));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ConfigurationHelper.disableOfflineContent(context);
        offlineContentHelper.clearOfflineContent(context);
    }

    private PlaylistDetailsScreen assertPlaylistContainsTrack(String trackTitle) {
        final CollectionScreen collectionScreen = mainNavHelper.goToCollections();
        PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.clickPlaylistsPreview()
                                                                      .scrollToAndClickPlaylistWithTitle(playlist);

        assertThat(playlistDetailsScreen.getTitle(), is(playlist));
        assertThat(playlistDetailsScreen.containsTrackWithTitle(trackTitle), is(true));

        return playlistDetailsScreen;
    }
}
