package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistDetailsMetadataBuilderTest extends AndroidUnitTest {
    @Mock private FeatureOperations featureOperations;
    @Mock private AccountOperations accountOperations;

    private final Playlist playlist = ModelFixtures.playlist();
    private final Track track1 = ModelFixtures.trackBuilder().build();
    private final Track track2 = ModelFixtures.trackBuilder().build();
    private final TrackItem trackItem1 = ModelFixtures.trackItem(track1);
    private final TrackItem trackItem2 = ModelFixtures.trackItem(track2);

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        PlaylistDetailsMetadata playlistDetailsViewModel = PlaylistDetailsMetadata.builder()
                                                                                  .with(resources(), featureOperations, accountOperations, playlist, newArrayList(trackItem1, trackItem2))
                                                                                  .with(OfflineState.NOT_OFFLINE)
                                                                                  .isRepostedByUser(false)
                                                                                  .isLikedByUser(true)
                                                                                  .isInEditMode(false)
                                                                                  .build();

        assertThat(playlistDetailsViewModel).isNotNull();
        assertThat(playlistDetailsViewModel.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsUpsell() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        PlaylistDetailsMetadata playlistDetailsViewModel = PlaylistDetailsMetadata.builder()
                                                                                  .with(resources(), featureOperations, accountOperations, playlist, newArrayList(trackItem1, trackItem2))
                                                                                  .with(OfflineState.NOT_OFFLINE)
                                                                                  .isRepostedByUser(false)
                                                                                  .isLikedByUser(true)
                                                                                  .isInEditMode(false)
                                                                                  .build();

        assertThat(playlistDetailsViewModel).isNotNull();
        assertThat(playlistDetailsViewModel.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.UPSELL);
    }

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsNone() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        PlaylistDetailsMetadata playlistDetailsViewModel = PlaylistDetailsMetadata.builder()
                                                                                  .with(resources(), featureOperations, accountOperations, playlist, newArrayList(trackItem1, trackItem2))
                                                                                  .with(OfflineState.NOT_OFFLINE)
                                                                                  .isRepostedByUser(false)
                                                                                  .isLikedByUser(true)
                                                                                  .isInEditMode(false)
                                                                                  .build();

        assertThat(playlistDetailsViewModel).isNotNull();
        assertThat(playlistDetailsViewModel.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.NONE);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() {
        Playlist playlist = ModelFixtures.playlistBuilder().trackCount(1).build();
        List<TrackItem> tracks = ModelFixtures.trackItems(2);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, tracks)
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.trackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        Playlist playlist = ModelFixtures.playlistBuilder().trackCount(1).build();

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.trackCount()).isEqualTo(1);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        Playlist playlist = ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build();
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.headerText()).isEqualTo("2 tracks, 1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        Playlist playlist = ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build();
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, ModelFixtures.trackItems(2))
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.headerText()).isEqualTo("2 tracks, 22:37");
    }

    @Test
    public void returnsOfflineAvailableNoneByDefault() throws Exception {
        Playlist playlist = ModelFixtures.playlist();

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.NONE);
    }

    @Test
    public void returnsOfflineAvailableForMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void returnsOfflineAvailableForLikedPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlistBuilder().isLikedByCurrentUser(true).build();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void returnsOfflineUpsell() throws Exception {
        Playlist playlist = ModelFixtures.playlistBuilder().build();
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(true)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.UPSELL);
    }

    @Test
    public void doNotShowOwnerOptionsForNotMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(false);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(false)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.isOwner()).isFalse();
    }

    @Test
    public void showOwnerOptionsForMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist, emptyList())
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(false)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.isOwner()).isTrue();
    }

    @Test
    public void cannotShuffleWithOneTrack() throws Exception {
        Playlist playlist1 = ModelFixtures.playlist();
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist1, ModelFixtures.trackItems(1))
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(false)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.canShuffle()).isFalse();
    }

    @Test
    public void canShuffleWithMoreThanOneTrack() throws Exception {
        Playlist playlist1 = ModelFixtures.playlist();
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.builder()
                                                                    .with(resources(), featureOperations, accountOperations, playlist1, ModelFixtures.trackItems(2))
                                                                    .with(OfflineState.NOT_OFFLINE)
                                                                    .isRepostedByUser(false)
                                                                    .isLikedByUser(false)
                                                                    .isInEditMode(false)
                                                                    .build();

        assertThat(item.canShuffle()).isTrue();
    }
}
