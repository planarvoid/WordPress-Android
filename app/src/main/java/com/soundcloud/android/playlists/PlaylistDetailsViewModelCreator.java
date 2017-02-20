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
                                           boolean isReposted,
                                           boolean isEditMode,
                                           Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists) {
        return create(playlist, trackItems, isLiked, isReposted, isEditMode, OfflineState.NOT_OFFLINE, otherPlaylists);
    }

    public PlaylistDetailsViewModel create(Playlist playlist,
                                           List<TrackItem> trackItems,
                                           boolean isLiked,
                                           boolean isReposted,
                                           boolean isEditMode,
                                           OfflineState offlineState,
                                           Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists) {

        Optional<PlaylistDetailUpsellItem> upsell = upsellOperations.getUpsell(playlist, trackItems);

        final PlaylistDetailTrackItem.Builder builder = PlaylistDetailTrackItem.builder().inEditMode(isEditMode);
        return PlaylistDetailsViewModel.builder()
                                       .metadata(createMetadata(playlist, trackItems, isLiked, isReposted, isEditMode, offlineState))
                                       .tracks(transform(trackItems, track -> builder.trackItem(track).build()))
                                       .upsell(upsell)
                                       .otherPlaylists(otherPlaylists)
                                       .build();
    }

    private PlaylistDetailsMetadata createMetadata(Playlist playlist, List<TrackItem> trackItems, boolean isLiked, boolean isReposted, boolean isEditMode, OfflineState offlineState) {
        int trackCount = getTrackCount(playlist, trackItems);
        PlaylistDetailsMetadata.OfflineOptions offlineOptions = getOfflineOptions();
        return PlaylistDetailsMetadata.from(playlist, trackItems, isLiked, isReposted, isEditMode, offlineState, trackCount, offlineOptions, resources, accountOperations.isLoggedInUser(playlist.creatorUrn()));
    }

    private int getTrackCount(Playlist playlist, List<TrackItem> trackItems) {
        return trackItems.isEmpty() ? playlist.trackCount() : trackItems.size();
    }

    private PlaylistDetailsMetadata.OfflineOptions getOfflineOptions() {
        if (featureOperations.isOfflineContentEnabled()) {
            return PlaylistDetailsMetadata.OfflineOptions.AVAILABLE;
        } else if (featureOperations.upsellOfflineContent()) {
            return PlaylistDetailsMetadata.OfflineOptions.UPSELL;
        } else {
            return PlaylistDetailsMetadata.OfflineOptions.NONE;
        }
    }
}
