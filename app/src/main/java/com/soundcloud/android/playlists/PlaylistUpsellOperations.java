package com.soundcloud.android.playlists;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.TieredTrack;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.ArrayList;
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

    public List<TypedListItem> toListItems(PlaylistWithTracks playlist) {
        if (!isUpsellEnabled() || playlist.isOwnedBy(accountOperations.getLoggedInUserUrn())) {
            return new ArrayList<TypedListItem>(playlist.getTracks());
        } else {
            return withUpsell(playlist.getTracks());
        }
    }

    private List<TypedListItem> withUpsell(List<TrackItem> tracks) {
        List<TypedListItem> items = new ArrayList<TypedListItem>(tracks);
        if (isUpsellEnabled()) {
            Optional<TypedListItem> upsellable = getFirstUpsellable(items);
            if (upsellable.isPresent()) {
                items.add(items.indexOf(upsellable.get()) + 1, UpsellListItem.forPlaylist());
            }
        }
        return items;
    }

    private Optional<TypedListItem> getFirstUpsellable(List<TypedListItem> streamItems) {
        for (TypedListItem streamItem : streamItems) {
            if (streamItem instanceof TieredTrack && isHighTierPreview((TieredTrack) streamItem)) {
                return Optional.of(streamItem);
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
