package com.soundcloud.android.search;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

class SearchPremiumItem implements ListItem {

    private final List<PropertySet> sourceSet;
    private final int resultsCount;

    SearchPremiumItem(List<PropertySet> sourceSetPremiumItems, int resultsCount) {
        this.sourceSet = sourceSetPremiumItems;
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
        return Urn.NOT_SET;
    }

    public List<PropertySet> getSourceSet() {
        return sourceSet;
    }

    public int getResultsCount() {
        return resultsCount;
    }
}
