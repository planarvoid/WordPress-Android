package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SearchResult implements Iterable<PropertySet> {
    private final List<PropertySet> items;
    final Optional<Link> nextHref;

    SearchResult(List<? extends PropertySetSource> items, Optional<Link> nextHref) {
        int emptyItems = 0;
        this.items = new ArrayList<>(items.size());
        for (PropertySetSource source : items) {
            if (source == null) {
                emptyItems++;
            } else {
                this.items.add(source.toPropertySet());
            }
        }
        this.nextHref = nextHref;
        if (emptyItems > 0) {
            ErrorUtils.handleSilentException(getMissingItemException(items, nextHref, emptyItems));
        }
    }

    private IllegalStateException getMissingItemException(List<? extends PropertySetSource> items, Optional<Link> nextHref, int emptyItems) {
        return new IllegalStateException(
                String.format(
                        "%d/%d empty items in search result with nextHref %s",
                        emptyItems,
                        items.size(),
                        (nextHref != null && nextHref.isPresent()) ? nextHref.get().getHref() : "none"));
    }

    @Override
    public Iterator<PropertySet> iterator() {
        return items.iterator();
    }

    public List<PropertySet> getItems() {
        return items;
    }
}
