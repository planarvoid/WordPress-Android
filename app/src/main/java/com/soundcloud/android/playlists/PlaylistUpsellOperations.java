package com.soundcloud.android.playlists;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.upsell.InlineUpsellOperations;
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

    List<PlaylistDetailItem> toListItems(PlaylistWithTracks playlist) {
        List<PlaylistDetailTrackItem> detailTrackItems = transform(playlist.getTracks(), PlaylistDetailTrackItem::new);
        if (!isUpsellEnabled() || playlist.isOwnedBy(accountOperations.getLoggedInUserUrn())) {
            return new ArrayList<>(detailTrackItems);
        } else {
            return withUpsell(detailTrackItems);
        }
    }

    private List<PlaylistDetailItem> withUpsell(List<PlaylistDetailTrackItem> tracks) {
        List<PlaylistDetailItem> items = new ArrayList<>(tracks);
        if (isUpsellEnabled()) {
            Optional<PlaylistDetailItem> upsellable = getFirstUpsellable(items);
            if (upsellable.isPresent()) {
                items.add(items.indexOf(upsellable.get()) + 1, new PlaylistDetailUpsellItem());
            }
        }
        return items;
    }

    private Optional<PlaylistDetailItem> getFirstUpsellable(List<PlaylistDetailItem> items) {
        for (PlaylistDetailItem item : items) {
            if (item instanceof PlaylistDetailTrackItem
                    && isHighTierPreview(((PlaylistDetailTrackItem) item).getTrackItem())) {
                return Optional.of(item);
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
