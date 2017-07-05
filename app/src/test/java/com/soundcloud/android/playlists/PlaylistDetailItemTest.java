package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Collections;

public class PlaylistDetailItemTest {

    @Test
    public void tracksWithDifferentUrnsAreNotTheSameItem() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem())
                                       .playlistUrn(Urn.forPlaylist(123L))
                                       .playlistOwnerUrn(Urn.forUser(123L))
                                       .build(),
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem())
                                       .playlistUrn(Urn.forPlaylist(123L))
                                       .playlistOwnerUrn(Urn.forUser(123L))
                                       .build()
        )).isFalse();
    }

    @Test
    public void tracksWithTheSameUrnsAreTheSameItem() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem(Urn.forTrack(1)))
                                       .playlistUrn(Urn.forPlaylist(123L))
                                       .playlistOwnerUrn(Urn.forUser(123L))
                                       .build(),
                PlaylistDetailTrackItem.builder().trackItem(ModelFixtures.trackItem(Urn.forTrack(1)))
                                       .playlistUrn(Urn.forPlaylist(123L))
                                       .playlistOwnerUrn(Urn.forUser(123L))
                                       .build()
        )).isTrue();
    }

    @Test
    public void otherItemsWithTheSameKindAreTheSame() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                new PlaylistDetailOtherPlaylistsItem("creatorName", Collections.emptyList(), false),
                new PlaylistDetailOtherPlaylistsItem("updatedCreatorName", Collections.emptyList(), false)
        )).isTrue();
    }

    @Test
    public void otherItemsWithTheDifferentKindsAreNotTheSame() throws Exception {
        assertThat(PlaylistDetailTrackItem.isTheSameItem(
                new PlaylistDetailOtherPlaylistsItem("creatorName", Collections.emptyList(), false),
                new PlaylistDetailUpsellItem(ModelFixtures.trackItem())
        )).isFalse();
    }
}
