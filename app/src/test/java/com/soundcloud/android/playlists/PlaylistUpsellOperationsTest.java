package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

public class PlaylistUpsellOperationsTest extends AndroidUnitTest {

    private final List<TrackItem> defaultTracks = Arrays.asList(
            PlayableFixtures.expectedTrackForListItem(Urn.forTrack(425L)),
            PlayableFixtures.expectedTrackForListItem(Urn.forTrack(752L)));
    private final Playlist playlist = ModelFixtures.playlist();
    @Mock private AccountOperations accountOperations;
    @Mock private InlineUpsellOperations upsellOperations;

    private PlaylistUpsellOperations operations;

    private final TrackItem track1 = PlayableFixtures.expectedTrackForListItem(Urn.forTrack(987L));
    private final TrackItem track2 = PlayableFixtures.upsellableTrack();
    private final TrackItem track3 = PlayableFixtures.upsellableTrack();
    private final List<TrackItem> upsellableTracks = Arrays.asList(track1, track2, track3);

    @Before
    public void setUp() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(567L));
        operations = new PlaylistUpsellOperations(accountOperations, upsellOperations);
    }

    @Test
    public void returnUpsellForFirstUpsellableTrack() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);

        final Optional<PlaylistDetailUpsellItem> upsell = operations.getUpsell(playlist, upsellableTracks);
        assertThat(upsell.get().track()).isEqualTo(track2);
    }

    @Test
    public void upsellIsAbsentWhenNoUpsellableTracksPresent() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);

        assertThat(operations.getUpsell(playlist, defaultTracks)).isEqualTo(Optional.absent());
    }

    @Test
    public void upsellIsAbsentWhenPlaylistIsOwnedByUser() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlist.creatorUrn());

        assertThat(operations.getUpsell(playlist, upsellableTracks)).isEqualTo(Optional.absent());
    }

    @Test
    public void upsellIsAbsentWhenDismissed() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(false);

        assertThat(operations.getUpsell(playlist, upsellableTracks)).isEqualTo(Optional.absent());
    }

    @Test
    public void forwardsUpsellDisabled() {
        operations.disableUpsell();

        verify(upsellOperations).disableInPlaylist();
    }
}
