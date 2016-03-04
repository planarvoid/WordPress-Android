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
    private final int resultsCount;
    final Optional<Link> nextHref;
    final Optional<Urn> queryUrn;
    private final Optional<SearchResult> premiumContent;

    private SearchResult(List<PropertySet> items, Optional<Link> nextHref, Optional<Urn> queryUrn,
                 Optional<SearchResult> premiumContent, int resultsCount) {
        this.items = items;
        this.resultsCount = resultsCount;
        this.nextHref = nextHref;
        this.queryUrn = queryUrn;
        this.premiumContent = premiumContent;
    }

    static SearchResult fromPropertySets(List<PropertySet> items, Optional<Link> nextHref, Urn queryUrn) {
        return new SearchResult(items, nextHref, Optional.of(queryUrn), Optional.<SearchResult>absent(), 0);
    }

    static SearchResult fromPropertySetSource(List<? extends PropertySetSource> items, Optional<Link> nextHref, Optional<Urn> queryUrn) {
        return fromPropertySetSource(items, nextHref, queryUrn, 0);
    }

    static SearchResult fromPropertySetSource(List<? extends PropertySetSource> items, Optional<Link> nextHref,
                                              Optional<Urn> queryUrn, int resultsCount) {
        return fromPropertySetSource(items, nextHref, queryUrn, Optional.<SearchResult>absent(), resultsCount);
    }

    static SearchResult fromPropertySetSource(List<? extends PropertySetSource> items, Optional<Link> nextHref,
                                              Optional<Urn> queryUrn, Optional<SearchResult> premiumContent,
                                              int resultsCount) {
        int emptyItems = 0;
        List<PropertySet> propertySets = new ArrayList<>(items.size());
        for (PropertySetSource source : items) {
            if (source == null) {
                emptyItems++;
            } else {
                propertySets.add(source.toPropertySet());
            }
        }
        if (emptyItems > 0) {
            ErrorUtils.handleSilentException(getMissingItemException(items, nextHref, emptyItems));
        }
        return new SearchResult(propertySets, nextHref, queryUrn, premiumContent, resultsCount);
    }

    private static IllegalStateException getMissingItemException(List<? extends PropertySetSource> items,
                                                                 Optional<Link> nextHref, int emptyItems) {
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

    List<PropertySet> getItems() {
        return items;
    }

    SearchResult addItem(int location, PropertySet propertySet) {
        items.add(location, propertySet);
        return this;
    }

    Optional<SearchResult> getPremiumContent() {
        return premiumContent;
    }

    int getResultsCount() {
        return resultsCount;
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
