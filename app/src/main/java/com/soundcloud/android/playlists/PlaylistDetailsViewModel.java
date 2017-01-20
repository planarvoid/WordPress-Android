package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

@AutoValue
abstract class PlaylistDetailsViewModel {

    public static PlaylistDetailsViewModel from(Playlist playlist, List<TrackItem> trackItems, boolean isLiked, Resources resources) {
        return builder()
                .header(PlaylistDetailHeaderItem.from(playlist, trackItems, isLiked, resources))
                .tracks(transform(trackItems, PlaylistDetailTrackItem::new))
                .build();
    }

    public static PlaylistDetailsViewModel.Builder builder() {
        return new AutoValue_PlaylistDetailsViewModel.Builder();
    }

    abstract PlaylistDetailHeaderItem header();

    abstract List<PlaylistDetailTrackItem> tracks();

    abstract Optional<PlaylistDetailUpsellItem> upsell();

    abstract Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists();

    List<PlaylistDetailItem> itemsWithHeader() {
        final ArrayList<PlaylistDetailItem> items = new ArrayList<>();
        addHeader(this, items);
        addTracksAndUpsell(this, items);
        addOtherPlaylists(this, items);
        return items;
    }

    List<PlaylistDetailItem> itemsWithoutHeader() {
        final ArrayList<PlaylistDetailItem> items = new ArrayList<>();
        addTracksAndUpsell(this, items);
        addOtherPlaylists(this, items);
        return items;
    }

    abstract Builder toBuilder();

    private static void addOtherPlaylists(PlaylistDetailsViewModel playlistDetailsViewModel, ArrayList<PlaylistDetailItem> items) {
        final Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists = playlistDetailsViewModel.otherPlaylists();
        if (otherPlaylists.isPresent()) {
            items.add(otherPlaylists.get());
        }
    }

    private static void addTracksAndUpsell(PlaylistDetailsViewModel playlistDetailsViewModel, ArrayList<PlaylistDetailItem> items) {
        for (PlaylistDetailTrackItem trackItem : playlistDetailsViewModel.tracks()) {
            items.add(trackItem);
            if (playlistDetailsViewModel.upsell().isPresent()) {
                final PlaylistDetailUpsellItem upsellItem = playlistDetailsViewModel.upsell().get();
                if (trackItem.getUrn().equals(upsellItem.track().getUrn())) {
                    items.add(upsellItem);
                }
            }
        }
    }

    private static void addHeader(PlaylistDetailsViewModel playlistDetailsViewModel, ArrayList<PlaylistDetailItem> items) {
        items.add(playlistDetailsViewModel.header());
    }

    @AutoValue.Builder
    abstract static class Builder {

        public Builder() {
            upsell(Optional.absent());
            otherPlaylists(Optional.absent());
        }

        abstract Builder header(PlaylistDetailHeaderItem value);

        abstract Builder tracks(List<PlaylistDetailTrackItem> value);

        Builder upsell(PlaylistDetailUpsellItem playlistDetailUpsellItem) {
            return upsell(Optional.of(playlistDetailUpsellItem));
        }

        abstract Builder upsell(Optional<PlaylistDetailUpsellItem> value);

        Builder otherPlaylists(PlaylistDetailOtherPlaylistsItem others) {
            return otherPlaylists(Optional.of(others));
        }

        abstract Builder otherPlaylists(Optional<PlaylistDetailOtherPlaylistsItem> value);

        abstract PlaylistDetailsViewModel build();
    }
}
