package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.UpsellListItem;
import org.junit.Test;

import java.util.List;

public class PlaylistDetailsViewModelTest extends AndroidUnitTest {

    private final Playlist playlist = ModelFixtures.playlist();
    private final TrackItem upsellableTrack = TestPropertySets.upsellableTrack();
    private final TrackItem defaultTrack = TestPropertySets.expectedTrackForListItem(Urn.forTrack(987L));
    private final List<TrackItem> trackItems = asList(defaultTrack, upsellableTrack);

    private final PlaylistDetailsViewModel model = PlaylistDetailsViewModel
            .builder()
            .header(PlaylistDetailHeaderItem.from(playlist, trackItems, false, resources()))
            .tracks(transform(trackItems, PlaylistDetailTrackItem::new))
            .upsell(new PlaylistDetailUpsellItem(new PlaylistDetailTrackItem(upsellableTrack).getTrackItem())).build();

    @Test
    public void insertsUpsellAfterFirstUpsellableTrack() {
        final List<PlaylistDetailItem> items = model.itemsWithoutHeader();

        assertThat(items.size()).isEqualTo(3);
        assertThat(((PlaylistDetailUpsellItem) items.get(2)).getUrn()).isEqualTo(UpsellListItem.PLAYLIST_UPSELL_URN);
    }
}
