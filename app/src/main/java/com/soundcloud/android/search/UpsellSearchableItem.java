package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.optional.Optional;

public class UpsellSearchableItem implements ListItem {

    static final Urn UPSELL_URN = new Urn("local:search:upsell");

    static UpsellSearchableItem forUpsell() {
        return new UpsellSearchableItem();
    }

    @Override
    public Urn getUrn() {
        return UPSELL_URN;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }
}
