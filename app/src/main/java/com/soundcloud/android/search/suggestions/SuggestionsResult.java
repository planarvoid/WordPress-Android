package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

class SuggestionsResult implements Iterable<PropertySet> {
    private final List<PropertySet> items;
    private final boolean isLocalResult;

    private SuggestionsResult(List<PropertySet> items, boolean isLocalResult) {
        this.items = items;
        this.isLocalResult = isLocalResult;
    }

    static SuggestionsResult fromPropertySets(List<PropertySet> items) {
        return new SuggestionsResult(items, true);
    }

    static SuggestionsResult fromPropertySetSource(List<? extends PropertySetSource> items) {
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
            ErrorUtils.handleSilentException(getMissingItemException(items, emptyItems));
        }
        return new SuggestionsResult(propertySets, false);
    }

    static SuggestionsResult emptyLocal() {
        return new SuggestionsResult(Collections.<PropertySet>emptyList(), true);
    }

    static SuggestionsResult emptyRemote() {
        return new SuggestionsResult(Collections.<PropertySet>emptyList(), false);
    }

    boolean isLocal() {
        return isLocalResult;
    }

    int size() {
        return (items != null) ? items.size() : 0;
    }

    private static IllegalStateException getMissingItemException(List<? extends PropertySetSource> items, int emptyItems) {
        return new IllegalStateException(
                String.format(Locale.getDefault(), "%d/%d empty items in suggestions result", emptyItems, items.size()));
    }

    @Override
    public Iterator<PropertySet> iterator() {
        return items.iterator();
    }

    List<PropertySet> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SuggestionsResult)) {
            return false;
        }
        final SuggestionsResult that = (SuggestionsResult) o;
        return MoreObjects.equal(items, that.items);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(items);
    }
}
