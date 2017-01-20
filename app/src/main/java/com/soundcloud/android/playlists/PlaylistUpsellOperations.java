package com.soundcloud.android.playlists;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.List;

class PlaylistUpsellOperations {

    private final AccountOperations accountOperations;
    private final InlineUpsellOperations upsellOperations;

    @Inject
    PlaylistUpsellOperations(AccountOperations accountOperations,
                             InlineUpsellOperations upsellOperations) {
        this.accountOperations = accountOperations;
        this.upsellOperations = upsellOperations;
    }

    Optional<PlaylistDetailUpsellItem> getUpsell(Playlist playlist, List<TrackItem> tracks) {
        final boolean isOwnedByLoggedInUser = playlist.creatorUrn().equals(accountOperations.getLoggedInUserUrn());
        if (!isUpsellEnabled() || isOwnedByLoggedInUser) {
            return Optional.absent();
        } else {
            final Optional<TrackItem> upsellable = getFirstUpsellableTrack(tracks);
            if (upsellable.isPresent()) {
                return Optional.of(new PlaylistDetailUpsellItem(upsellable.get()));
            } else {
                return Optional.absent();
            }
        }
    }

    private Optional<TrackItem> getFirstUpsellableTrack(List<TrackItem> tracks) {
        for (TrackItem track : tracks) {
            if (isHighTierPreview(track)) {
                return Optional.of(track);
            }
        }
        return Optional.absent();
    }

    private boolean isUpsellEnabled() {
        return upsellOperations.shouldDisplayInPlaylist();
    }

    public void disableUpsell() {
        upsellOperations.disableInPlaylist();
    }

}
