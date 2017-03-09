package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Collections;

public class PlaylistDetailItemTest extends AndroidUnitTest {

    @Test
    public void tracksWithDifferentUrnsAreNotTheSameItem() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem()).build(),
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem()).build()
        )).isFalse();
    }

    @Test
    public void tracksWithTheSameUrnsAreTheSameItem() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem(Urn.forTrack(1))).build(),
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem(Urn.forTrack(1))).build()
        )).isTrue();
    }

    @Test
    public void otherItemsWithTheSameKindAreTheSame() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                new PlaylistDetailOtherPlaylistsItem("creatorName", Collections.emptyList()),
                new PlaylistDetailOtherPlaylistsItem("updatedCreatorName", Collections.emptyList())
        )).isTrue();
    }

    @Test
    public void otherItemsWithTheDifferentKindsAreNotTheSame() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                new PlaylistDetailOtherPlaylistsItem("creatorName", Collections.emptyList()),
                new PlaylistDetailUpsellItem(ModelFixtures.trackItem())
        )).isFalse();
    }
}
