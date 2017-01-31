package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import javax.inject.Inject;
import java.util.List;

class PlaylistDetailsViewModelCreator {

    private final Resources resources;
    private final FeatureOperations featureOperations;
    private final AccountOperations accountOperations;
    private final PlaylistUpsellOperations upsellOperations;

    @Inject
    public PlaylistDetailsViewModelCreator(Resources resources,
                                           FeatureOperations featureOperations,
                                           AccountOperations accountOperations,
                                           PlaylistUpsellOperations upsellOperations) {
        this.resources = resources;
        this.featureOperations = featureOperations;
        this.accountOperations = accountOperations;
        this.upsellOperations = upsellOperations;
    }

    public PlaylistDetailsViewModel create(Playlist playlist,
                                           List<TrackItem> trackItems,
                                           boolean isLiked,
                                           boolean isEditMode,
                                           Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists) {
        return create(playlist, trackItems, isLiked, isEditMode, OfflineState.NOT_OFFLINE, otherPlaylists);
    }

        public PlaylistDetailsViewModel create(Playlist playlist,
                                           List<TrackItem> trackItems,
                                           boolean isLiked,
                                           boolean isEditMode,
                                           OfflineState offlineState,
                                           Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists) {

        Optional<PlaylistDetailUpsellItem> upsell = upsellOperations.getUpsell(playlist, trackItems);
        return PlaylistDetailsViewModel.builder()
                                       .metadata(createMetadata(playlist, trackItems, isLiked, isEditMode, offlineState))
                                       .tracks(transform(trackItems, PlaylistDetailTrackItem::new))
                                       .upsell(upsell)
                                       .otherPlaylists(otherPlaylists)
                                       .build();
    }

    private PlaylistDetailsMetadata createMetadata(Playlist playlist, List<TrackItem> trackItems, boolean isLiked, boolean isEditMode, OfflineState offlineState) {
        int trackCount = getTrackCount(playlist, trackItems);
        PlaylistDetailsMetadata.OfflineOptions offlineOptions = getOfflineOptions(playlist);
        return PlaylistDetailsMetadata.from(playlist, trackItems, isLiked, isEditMode, offlineState, trackCount, offlineOptions, resources, accountOperations.isLoggedInUser(playlist.creatorUrn()));
    }

    private int getTrackCount(Playlist playlist, List<TrackItem> trackItems) {
        return trackItems.isEmpty() ? playlist.trackCount() : trackItems.size();
    }

    private PlaylistDetailsMetadata.OfflineOptions getOfflineOptions(Playlist playlist) {
        if (featureOperations.isOfflineContentEnabled() && (accountOperations.isLoggedInUser(playlist.creatorUrn()) || playlist.isLikedByCurrentUser().or(false))) {
            return PlaylistDetailsMetadata.OfflineOptions.AVAILABLE;
        } else if (featureOperations.upsellOfflineContent()) {
            return PlaylistDetailsMetadata.OfflineOptions.UPSELL;
        } else {
            return PlaylistDetailsMetadata.OfflineOptions.NONE;
        }
    }
}
