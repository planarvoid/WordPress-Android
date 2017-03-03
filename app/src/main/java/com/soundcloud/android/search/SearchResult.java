package com.soundcloud.android.search;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SearchResult implements Iterable<ListItem> {
    private final List<ListItem> items;
    private final int resultsCount;
    final Optional<Link> nextHref;
    final Optional<Urn> queryUrn;
    private final Optional<SearchResult> premiumContent;

    private SearchResult(List<ListItem> items, Optional<Link> nextHref, Optional<Urn> queryUrn,
                         Optional<SearchResult> premiumContent, int resultsCount) {
        this.items = items;
        this.resultsCount = resultsCount;
        this.nextHref = nextHref;
        this.queryUrn = queryUrn;
        this.premiumContent = premiumContent;
    }

    static SearchResult fromSearchableItems(List<ListItem> items, Optional<Link> nextHref, Urn queryUrn) {
        return new SearchResult(items, nextHref, Optional.of(queryUrn), Optional.absent(), 0);
    }

    static SearchResult fromSearchableItems(List<? extends ListItem> items,
                                            Optional<Link> nextHref,
                                            Optional<Urn> queryUrn) {
        return fromSearchableItems(items, nextHref, queryUrn, 0);
    }

    static SearchResult fromSearchableItems(List<? extends ListItem> items, Optional<Link> nextHref,
                                            Optional<Urn> queryUrn, int resultsCount) {
        return fromSearchableItems(items, nextHref, queryUrn, Optional.absent(), resultsCount);
    }

    static SearchResult fromSearchableItems(List<? extends ListItem> items, Optional<Link> nextHref,
                                            Optional<Urn> queryUrn, Optional<SearchResult> premiumContent,
                                            int resultsCount) {
        int emptyItems = 0;
        List<ListItem> nonNullItems = new ArrayList<>(items.size());
        for (ListItem source : items) {
            if (source == null) {
                emptyItems++;
            } else {
                nonNullItems.add(source);
            }
        }
        if (emptyItems > 0) {
            ErrorUtils.handleSilentException(getMissingItemException(items, nextHref, emptyItems));
        }
        return new SearchResult(nonNullItems, nextHref, queryUrn, premiumContent, resultsCount);
    }

    SearchResult copyWithSearchableItems(List<ListItem> items) {
        return new SearchResult(items, this.nextHref, this.queryUrn, this.premiumContent, this.resultsCount);
    }

    private static IllegalStateException getMissingItemException(List<? extends ListItem> items,
                                                                 Optional<Link> nextHref, int emptyItems) {
        return new IllegalStateException(
                String.format(
                        Locale.getDefault(),
                        "%d/%d empty items in search result with nextHref %s",
                        emptyItems,
                        items.size(),
                        (nextHref != null && nextHref.isPresent()) ? nextHref.get().getHref() : "none"));
    }

    @Override
    public Iterator<ListItem> iterator() {
        return items.iterator();
    }

    List<ListItem> getItems() {
        return items;
    }

    SearchResult addItem(int location, ListItem searchableItem) {
        items.add(location, searchableItem);
        return this;
    }

    Optional<SearchResult> getPremiumContent() {
        return premiumContent;
    }

    int getResultsCount() {
        return resultsCount;
    }

    Urn getFirstItemUrn() {
        return (getItems().isEmpty()) ? Urn.NOT_SET : getItems().get(0).getUrn();
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
