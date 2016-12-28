package com.soundcloud.android.api.model;

import com.soundcloud.java.functions.Function;

import java.util.List;

public class PagedRemoteCollection<T> extends PagedCollection<T> {

    public PagedRemoteCollection(ModelCollection<T> items) {
        super(items);
    }

    public PagedRemoteCollection(List<T> items, String nextPage) {
        this(new ModelCollection<>(items, nextPage));
    }

    public <S> PagedRemoteCollection<S> transform(Function<T, S> function) {
        return new PagedRemoteCollection<>(items.transform(function));
    }
}
