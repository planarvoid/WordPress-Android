package com.soundcloud.android.playlists;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistDetailViewModelCreatorTest extends AndroidUnitTest {

    private PlaylistDetailsViewModelCreator creator;

    @Mock private FeatureOperations featureOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaylistUpsellOperations upsellOperations;

    @Before
    public void setUp() throws Exception {
        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(Optional.absent());
        creator = new PlaylistDetailsViewModelCreator(resources(), featureOperations, accountOperations, upsellOperations);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() {
        Playlist playlist = ModelFixtures.playlistBuilder().trackCount(1).build();
        List<TrackItem> tracks = ModelFixtures.trackItems(2);

        final PlaylistDetailsViewModel item = creator.create(playlist, tracks, true, false, false, Optional.absent());

        assertThat(item.metadata().trackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        Playlist playlist = ModelFixtures.playlistBuilder().trackCount(1).build();

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().trackCount()).isEqualTo(1);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        Playlist playlist = ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build();
        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().headerText()).isEqualTo("2 tracks, 1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        Playlist playlist = ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build();
        final PlaylistDetailsViewModel item = creator.create(playlist, ModelFixtures.trackItems(2), true, false, false, Optional.absent());

        assertThat(item.metadata().headerText()).isEqualTo("2 tracks, 22:37");
    }

    @Test
    public void returnsOfflineAvailableNoneByDefault() throws Exception {
        Playlist playlist = ModelFixtures.playlist();

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.NONE);
    }

    @Test
    public void returnsOfflineAvailableForMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void returnsOfflineAvailableForLikedPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlistBuilder().isLikedByCurrentUser(true).build();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void returnsOfflineUpsell() throws Exception {
        Playlist playlist = ModelFixtures.playlistBuilder().build();
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), true, false, false, Optional.absent());

        assertThat(item.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.UPSELL);
    }

    @Test
    public void doNotShowOwnerOptionsForNotMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(false);

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), false, false, false, Optional.absent());

        assertThat(item.metadata().showOwnerOptions()).isFalse();
    }

    @Test
    public void showOwnerOptionsForMyPlaylist() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        final PlaylistDetailsViewModel item = creator.create(playlist, emptyList(), false, false, false, Optional.absent());

        assertThat(item.metadata().showOwnerOptions()).isTrue();
    }

    @Test
    public void cannotShuffleWithOneTrack() throws Exception {

        final PlaylistDetailsViewModel item = creator.create(ModelFixtures.playlist(), ModelFixtures.trackItems(1), false, false, false, Optional.absent());

        assertThat(item.metadata().canShuffle()).isFalse();
    }

    @Test
    public void canShuffleWithMoreThanOneTrack() throws Exception {
        final PlaylistDetailsViewModel item = creator.create(ModelFixtures.playlist(), ModelFixtures.trackItems(2), false, false, false, Optional.absent());

        assertThat(item.metadata().canShuffle()).isTrue();
    }
}
