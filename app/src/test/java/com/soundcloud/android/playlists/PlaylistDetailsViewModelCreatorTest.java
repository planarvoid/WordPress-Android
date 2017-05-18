package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaylistDetailsViewModelCreatorTest extends AndroidUnitTest {
    @Mock private FeatureOperations featureOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaylistUpsellOperations upsellOperations;

    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
    private final Playlist playlist = ModelFixtures.playlist();
    private final Track track1 = ModelFixtures.trackBuilder().build();
    private final Track track2 = ModelFixtures.trackBuilder().build();
    private final TrackItem trackItem1 = ModelFixtures.trackItem(track1);
    private final TrackItem trackItem2 = ModelFixtures.trackItem(track2);

    private PlaylistDetailsViewModelCreator playlistDetailsViewModelCreator;

    @Before
    public void setUp() {
        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(Optional.absent());

        playlistDetailsViewModelCreator = new PlaylistDetailsViewModelCreator(
                resources(),
                featureOperations,
                accountOperations,
                upsellOperations);
    }

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsAvailable() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        PlaylistDetailsViewModel playlistDetailsViewModel = playlistDetailsViewModelCreator.create(playlist, newArrayList(trackItem1, trackItem2), true, false, false, Optional.of(createOtherPlaylistItem()));

        assertThat(playlistDetailsViewModel.metadata()).isNotNull();
        assertThat(playlistDetailsViewModel.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.AVAILABLE);
    }

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsUpsell() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        PlaylistDetailsViewModel playlistDetailsViewModel = playlistDetailsViewModelCreator.create(playlist, newArrayList(trackItem1, trackItem2), true, false, false, Optional.of(createOtherPlaylistItem()));

        assertThat(playlistDetailsViewModel.metadata()).isNotNull();
        assertThat(playlistDetailsViewModel.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.UPSELL);
    }

    @Test
    public void shouldCreatePlaylistDetailsViewModelWithOfflineOptionsNone() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        PlaylistDetailsViewModel playlistDetailsViewModel = playlistDetailsViewModelCreator.create(playlist, newArrayList(trackItem1, trackItem2), true, false, false, Optional.of(createOtherPlaylistItem()));

        assertThat(playlistDetailsViewModel.metadata()).isNotNull();
        assertThat(playlistDetailsViewModel.metadata().offlineOptions()).isEqualTo(PlaylistDetailsMetadata.OfflineOptions.NONE);
    }

    private PlaylistDetailOtherPlaylistsItem createOtherPlaylistItem() {
        return new PlaylistDetailOtherPlaylistsItem(
                playlist.creatorName(), singletonList(ModelFixtures.playlistItem(playlistPost.getApiPlaylist())), playlist.isAlbum());
    }
}
