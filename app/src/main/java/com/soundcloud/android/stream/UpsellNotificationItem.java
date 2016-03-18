package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.Kind.UPSELL;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

class UpsellNotificationItem implements StreamItem {
    static final Urn URN = new Urn("soundcloud:notifications:stream-upsell");

    private final Date CREATED_AT = new Date();

    @Override
    public ListItem update(PropertySet sourceSet) {
        return this;
    }

    @Override
    public Urn getUrn() {
        return URN;
    }

    @Override
    public Optional<String> getArtworkUrlTemplate() {
        return Optional.absent();
    }

    @Override
    public Kind getKind() {
        return UPSELL;
    }

    @Override
    public Date getCreatedAt() {
        return CREATED_AT;
    }
}
