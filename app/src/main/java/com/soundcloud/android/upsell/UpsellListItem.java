package com.soundcloud.android.upsell;

import static com.soundcloud.android.presentation.TypedListItem.Kind.UPSELL;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public class UpsellListItem implements TypedListItem {

    public static final Urn STREAM_UPSELL_URN = new Urn("soundcloud:notifications:stream-upsell");
    public static final Urn PLAYLIST_UPSELL_URN = new Urn("soundcloud:notifications:playlist-upsell");

    private final Date createdAt;
    private final Urn urn;

    public static UpsellListItem forStream() {
        return new UpsellListItem(STREAM_UPSELL_URN);
    }

    public static UpsellListItem forPlaylist() {
        return new UpsellListItem(PLAYLIST_UPSELL_URN);
    }

    private UpsellListItem(Urn urn) {
        this.urn = urn;
        createdAt = new Date();
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        return this;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    @Override
    public Kind getKind() {
        return UPSELL;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

}