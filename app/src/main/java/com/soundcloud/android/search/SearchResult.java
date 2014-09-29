package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.PropertySetSource;

import java.util.Iterator;
import java.util.List;

class SearchResult implements Iterable {
    final List<? extends PropertySetSource> items;
    final Optional<Link> nextHref;

    SearchResult(List<? extends PropertySetSource> items, Optional<Link> nextHref) {
        this.items = items;
        this.nextHref = nextHref;
    }

    @Override
    public Iterator iterator() {
        return items.iterator();
    }
}
