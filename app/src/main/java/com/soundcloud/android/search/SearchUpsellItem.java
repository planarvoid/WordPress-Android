package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.optional.Optional;

class SearchUpsellItem implements ListItem {

    static final Urn UPSELL_URN = new Urn("local:search:upsell");

    @Override
    public Urn getUrn() {
        return UPSELL_URN;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }
}
