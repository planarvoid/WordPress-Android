package com.soundcloud.android.playlists;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.List;

public class PlaylistDetailsViewModelTest extends AndroidUnitTest {

    private final Playlist playlist = ModelFixtures.playlist();
    private final TrackItem upsellableTrack = PlayableFixtures.upsellableTrack();
    private final TrackItem defaultTrack = PlayableFixtures.expectedTrackForListItem(Urn.forTrack(987L));
    private final List<TrackItem> trackItems = asList(defaultTrack, upsellableTrack);

    private final PlaylistDetailTrackItem playlistDetailTrackItem = PlaylistDetailTrackItem.builder().trackItem(upsellableTrack)
                                                                                           .playlistUrn(Urn.forPlaylist(123L))
                                                                                           .playlistOwnerUrn(Urn.forUser(123L))
                                                                                           .build();

    private final PlaylistDetailsViewModel model = PlaylistDetailFixtures.createWithUpsell(
            resources(), playlist, trackItems, Optional.of(new PlaylistDetailUpsellItem(playlistDetailTrackItem.trackItem())));

    @Test
    public void insertsUpsellAfterFirstUpsellableTrack() {
        final List<PlaylistDetailItem> items = model.itemsWithoutHeader();

        assertThat(items.size()).isEqualTo(3);
        assertThat(((PlaylistDetailUpsellItem) items.get(2)).getUrn()).isEqualTo(UpsellListItem.PLAYLIST_UPSELL_URN);
    }
}
