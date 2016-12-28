package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.SearchableItem;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

public final class PropertySets {

    @SuppressWarnings({"PMD.LooseCoupling"}) // we need ArrayList for Parceling
    public static ArrayList<Urn> extractUrns(List<? extends SearchableItem> entities) {
        ArrayList<Urn> urns = new ArrayList<>(entities.size());
        for (SearchableItem searchableItem : entities) {
            urns.add(searchableItem.getUrn());
        }
        return urns;
    }

    public static List<Long> extractIds(Iterable<Urn> urns, Optional<Predicate<Urn>> predicate) {
        final List<Long> ids = new ArrayList<>(Iterables.size(urns));

        for (Urn urn : urns) {
            if (!predicate.isPresent() || predicate.get().apply(urn)) {
                ids.add(urn.getNumericId());
            }
        }
        return ids;
    }

    private PropertySets() {
        // no instances
    }
}
