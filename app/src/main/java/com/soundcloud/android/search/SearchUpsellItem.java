package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

class SearchUpsellItem implements ListItem {

    static final Urn UPSELL_URN = new Urn("local:search:upsell");

    @Override
    public ListItem update(PropertySet updatedProperties) {
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return UPSELL_URN;
    }
}
