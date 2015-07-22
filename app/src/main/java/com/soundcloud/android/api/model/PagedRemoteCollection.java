package com.soundcloud.android.api.model;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PagedRemoteCollection implements Iterable<PropertySet> {

    private final Optional<String> nextPage;
    private final List<PropertySet> items;

    public PagedRemoteCollection(ModelCollection<? extends PropertySetSource> scModels) {
        this(scModels.getCollection(),
                scModels.getNextLink().isPresent() ? scModels.getNextLink().get().getHref() : null);
    }

    public PagedRemoteCollection(List<? extends PropertySetSource> items, String nextPage) {
        this.nextPage = Optional.fromNullable(nextPage);
        this.items = new ArrayList<>(items.size());
        for (PropertySetSource source : items){
            this.items.add(source.toPropertySet());
        }
    }

    public Optional<String> nextPageLink(){
        return nextPage;
    }

    @Override
    public Iterator<PropertySet> iterator() {
        return items.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PagedRemoteCollection)) {
            return false;
        }
        PagedRemoteCollection that = (PagedRemoteCollection) o;
        return items.equals(that.items) && nextPage.equals(that.nextPage);
    }

    @Override
    public int hashCode() {
        int result = nextPage.hashCode();
        result = 31 * result + items.hashCode();
        return result;
    }
}
