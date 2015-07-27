package com.soundcloud.android.search;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SearchResult implements Iterable<PropertySet> {
    private final List<PropertySet> items;
    final Optional<Link> nextHref;
    final Optional<Urn> queryUrn;

    SearchResult(List<? extends PropertySetSource> items, Optional<Link> nextHref, Optional<Urn> queryUrn) {
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
        this.queryUrn = queryUrn;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchResult)) {
            return false;
        }
        SearchResult that = (SearchResult) o;
        return MoreObjects.equal(items, that.items) &&
                MoreObjects.equal(nextHref, that.nextHref) &&
                MoreObjects.equal(queryUrn, that.queryUrn);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(items, nextHref, queryUrn);
    }
}
