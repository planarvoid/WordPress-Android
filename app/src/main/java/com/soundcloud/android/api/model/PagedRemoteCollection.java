package com.soundcloud.android.api.model;

import com.google.common.base.Optional;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PagedRemoteCollection implements Iterable<PropertySet> {

    private final Optional<String> nextPage;
    private final List<PropertySet> items;

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
}
