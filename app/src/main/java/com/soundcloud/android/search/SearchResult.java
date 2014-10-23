package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SearchResult implements Iterable<PropertySet> {
    private final List<PropertySet> items;
    final Optional<Link> nextHref;

    SearchResult(List<? extends PropertySetSource> items, Optional<Link> nextHref) {
        this.items = new ArrayList<>(items.size());
        for (PropertySetSource source : items) {
            this.items.add(source.toPropertySet());
        }
        this.nextHref = nextHref;
    }

    @Override
    public Iterator<PropertySet> iterator() {
        return items.iterator();
    }

    public List<PropertySet> getItems() {
        return items;
    }
}
