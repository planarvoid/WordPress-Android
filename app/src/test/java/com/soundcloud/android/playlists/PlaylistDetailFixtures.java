package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import java.util.List;

class PlaylistDetailFixtures {

    static PlaylistDetailsViewModel create(Resources resources, Playlist playlist, List<TrackItem> trackItems) {
        return create(resources, playlist, trackItems, Optional.absent(), Optional.absent());
    }

    static PlaylistDetailsViewModel create(Resources resources, Playlist playlist, List<TrackItem> trackItems, PlaylistDetailOtherPlaylistsItem otherPlaylistsByUser) {
        return create(resources, playlist, trackItems, Optional.of(otherPlaylistsByUser), Optional.absent());
    }

    static PlaylistDetailsViewModel createWithOtherMyPlaylists(Resources resources, Playlist playlist, List<TrackItem> trackItems, Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists) {
        PlaylistDetailsViewModel viewModel = create(resources, playlist, trackItems, otherPlaylists, Optional.absent());
        return viewModel
                .toBuilder()
                .metadata(viewModel.metadata().toBuilder().isOwner(true).build())
                .build();
    }

    static PlaylistDetailsViewModel createWithUpsell(Resources resources, Playlist playlist, List<TrackItem> trackItems, Optional<PlaylistDetailUpsellItem> upsellItemOptional) {
        return create(resources, playlist, trackItems, Optional.absent(), upsellItemOptional);
    }

    private static PlaylistDetailsViewModel create(Resources resources, Playlist playlist, List<TrackItem> trackItems, Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists, Optional<PlaylistDetailUpsellItem> upsellItemOptional) {
        return PlaylistDetailsViewModel.builder()
                                       .metadata(PlaylistDetailsMetadata.from(playlist,
                                                                              trackItems,
                                                                              false,
                                                                              false,
                                                                              false,
                                                                              playlist.offlineState().or(OfflineState.NOT_OFFLINE),
                                                                              trackItems.isEmpty() ? playlist.trackCount() : trackItems.size(),
                                                                              PlaylistDetailsMetadata.OfflineOptions.NONE,
                                                                              resources,
                                                                              false))
                                       .tracks(transform(trackItems, trackItem -> PlaylistDetailTrackItem.builder().trackItem(trackItem).build()))
                                       .upsell(upsellItemOptional)
                                       .otherPlaylists(otherPlaylists)
                                       .build();
    }

}
