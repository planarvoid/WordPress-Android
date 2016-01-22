package com.soundcloud.android.search;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class SearchPremiumItem implements ListItem {

    static final Urn PREMIUM_URN = new Urn("local:search:premium");

    private final List<PropertySet> sourceSet;
    private final Optional<Link> nextHref;
    private final int resultsCount;

    SearchPremiumItem(List<PropertySet> sourceSetPremiumItems, Optional<Link> nextHref, int resultsCount) {
        this.sourceSet = sourceSetPremiumItems;
        this.nextHref = nextHref;
        this.resultsCount = resultsCount;
    }

    @Override
    public ListItem update(PropertySet updatedProperties) {
        for (PropertySet propertySet : sourceSet) {
            if (propertySet.get(EntityProperty.URN).equals(updatedProperties.get(EntityProperty.URN))) {
                propertySet.update(updatedProperties);
                return this;
            }
        }
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return PREMIUM_URN;
    }

    List<PropertySet> getSourceSet() {
        return sourceSet;
    }

    Optional<Link> getNextHref() {
        return nextHref;
    }

    int getResultsCount() {
        return resultsCount;
    }
}
