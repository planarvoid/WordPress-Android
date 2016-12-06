package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.android.upsell.UpsellListItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

public class PlaylistUpsellOperationsTest extends AndroidUnitTest {

    @Mock private AccountOperations accountOperations;
    @Mock private InlineUpsellOperations upsellOperations;

    private PlaylistUpsellOperations operations;

    private final TrackItem track1 = TrackItem.from(TestPropertySets.expectedTrackForListItem(Urn.forTrack(987L)));
    private final TrackItem track2 = TrackItem.from(TestPropertySets.upsellableTrack());
    private final TrackItem track3 = TrackItem.from(TestPropertySets.upsellableTrack());

    @Before
    public void setUp() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(567L));
        operations = new PlaylistUpsellOperations(accountOperations, upsellOperations);
    }

    @Test
    public void insertsUpsellAfterFirstUpsellableTrack() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);

        final List<PlaylistDetailItem> items = operations.toListItems(upsellablePlaylist());

        assertThat(items.size()).isEqualTo(4);
        assertThat(((PlaylistDetailUpsellItem) items.get(2)).getUrn()).isEqualTo(UpsellListItem.PLAYLIST_UPSELL_URN);
    }

    @Test
    public void doesNotInsertUpsellIfNoUpsellableTracksPresent() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);

        final List<PlaylistDetailItem> items = operations.toListItems(defaultPlaylist());

        assertThat(items.size()).isEqualTo(2);
    }

    @Test
    public void doesNotInsertUpsellInOwnedPlaylist() {
        final PlaylistWithTracks playlist = upsellablePlaylist();
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlist.getCreatorUrn());

        final List<PlaylistDetailItem> items = operations.toListItems(playlist);

        assertOriginalTracks(items);
    }

    @Test
    public void doesNotInsertUpsellIfDismissed() {
        when(upsellOperations.shouldDisplayInPlaylist()).thenReturn(false);

        final List<PlaylistDetailItem> items = operations.toListItems(upsellablePlaylist());

        assertOriginalTracks(items);
    }

    @Test
    public void forwardsUpsellDisabled() {
        operations.disableUpsell();

        verify(upsellOperations).disableInPlaylist();
    }

    private PlaylistWithTracks defaultPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        return new PlaylistWithTracks(playlist.toPropertySet(), Arrays.asList(
                TrackItem.from(TestPropertySets.expectedTrackForListItem(Urn.forTrack(425L))),
                TrackItem.from(TestPropertySets.expectedTrackForListItem(Urn.forTrack(752L)))));
    }

    private PlaylistWithTracks upsellablePlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        return new PlaylistWithTracks(playlist.toPropertySet(), Arrays.asList(track1, track2, track3));
    }

    void assertOriginalTracks(List<PlaylistDetailItem> items) {
        assertThat(items).containsExactly(new PlaylistDetailTrackItem(track1),
                                          new PlaylistDetailTrackItem(track2),
                                          new PlaylistDetailTrackItem(track3));
    }

}
