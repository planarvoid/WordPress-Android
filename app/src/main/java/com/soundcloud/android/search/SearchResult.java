package com.soundcloud.android.search;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import java.util.Iterator;
import java.util.List;

class SearchResult implements Iterable<PropertySet> {
    final List<PropertySet> items;
    final Optional<Link> nextHref;

    SearchResult(List<? extends PropertySetSource> items, Optional<Link> nextHref) {
        this.items = Lists.transform(items, new Function<PropertySetSource, PropertySet>() {
            @Override
            public PropertySet apply(PropertySetSource input) {
                return input.toPropertySet();
            }
        });
        this.nextHref = nextHref;
    }

    @Override
    public Iterator<PropertySet> iterator() {
        return items.iterator();
    }
}
