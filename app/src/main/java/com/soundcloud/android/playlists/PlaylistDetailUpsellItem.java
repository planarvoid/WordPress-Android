package com.soundcloud.android.playlists;

import static com.soundcloud.android.presentation.TypedListItem.Kind.UPSELL;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

class PlaylistDetailUpsellItem extends PlaylistDetailItem implements TypedListItem {

    private final Date createdAt;
    private final TrackItem track;

    PlaylistDetailUpsellItem(TrackItem track) {
        super(PlaylistDetailItem.Kind.UpsellItem);
        this.track = track;
        this.createdAt = new Date();
    }

    @Override
    public Urn getUrn() {
        return UpsellListItem.PLAYLIST_UPSELL_URN;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    @Override
    public TypedListItem.Kind getKind() {
        return UPSELL;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    public TrackItem track() {
        return track;
    }
}
