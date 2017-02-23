package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

@AutoValue
abstract class PlaylistDetailsViewModel {

    public static PlaylistDetailsViewModel.Builder builder() {
        return new AutoValue_PlaylistDetailsViewModel.Builder();
    }

    abstract PlaylistDetailsMetadata metadata();

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

    List<PlaylistDetailItem> itemsWithHeader(PlaylistDetailEmptyItem emptyItem) {
        final ArrayList<PlaylistDetailItem> items = new ArrayList<>();
        addHeader(this, items);
        addEmptyOrTracks(emptyItem, items);
        addOtherPlaylists(this, items);
        return items;
    }

    List<PlaylistDetailItem> itemsWithoutHeader() {
        final ArrayList<PlaylistDetailItem> items = new ArrayList<>();
        addTracksAndUpsell(this, items);
        addOtherPlaylists(this, items);
        return items;
    }

    List<PlaylistDetailItem> itemsWithoutHeader(PlaylistDetailEmptyItem emptyItem) {
        final ArrayList<PlaylistDetailItem> items = new ArrayList<>();
        addEmptyOrTracks(emptyItem, items);
        addOtherPlaylists(this, items);
        return items;
    }

    private void addEmptyOrTracks(PlaylistDetailEmptyItem emptyItem, ArrayList<PlaylistDetailItem> items) {
        if (tracks().isEmpty()) {
            items.add(emptyItem);
        } else {
            addTracksAndUpsell(this, items);
        }
    }

    public PlaylistDetailsViewModel updateWithMarkedForOffline(boolean value) {
        return toBuilder().metadata(metadata().toBuilder().isMarkedForOffline(value).build()).build();
    }

    abstract Builder toBuilder();

    private static void addOtherPlaylists(PlaylistDetailsViewModel playlistDetailsViewModel, ArrayList<PlaylistDetailItem> items) {
        final Optional<PlaylistDetailOtherPlaylistsItem> otherPlaylists = playlistDetailsViewModel.otherPlaylists();
        if (otherPlaylists.isPresent()) {
            items.add(otherPlaylists.get());
        }
    }

    private static void addTracksAndUpsell(PlaylistDetailsViewModel playlistDetailsViewModel, ArrayList<PlaylistDetailItem> items) {
        List<PlaylistDetailTrackItem> tracks = playlistDetailsViewModel.tracks();
            for (PlaylistDetailTrackItem trackItem : tracks) {
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
        items.add(new PlaylistDetailsHeaderItem(Optional.of(playlistDetailsViewModel.metadata())));
    }

    @AutoValue.Builder
    abstract static class Builder {

        public Builder() {
            upsell(Optional.absent());
            otherPlaylists(Optional.absent());
        }

        abstract Builder metadata(PlaylistDetailsMetadata value);

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