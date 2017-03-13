package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.api.legacy.json.Views;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * Holder for data returned in the API's "linked_partitioning" format (/tracks?linked_partitioning=1)
 *
 * @param <T>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionHolder<T> implements Iterable<T> {

    @JsonProperty
    @JsonView(Views.Mini.class)
    public List<T> collection;

    @JsonProperty @JsonView(Views.Mini.class)
    public String next_href;

    public CollectionHolder() {
        this(Collections.emptyList());
    }

    public CollectionHolder(List<T> collection) {
        this.collection = new ArrayList<>(collection);
    }

    public CollectionHolder(List<T> collection, String nextHref) {
        this(collection);
        next_href = nextHref;
    }

    /**
     * @noinspection unchecked
     */
    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    public boolean add(T item) {
        return collection.add(item);
    }

    public T get(int index) {
        return collection.get(index);
    }

    public List<T> getCollection() {
        return Collections.unmodifiableList(collection);
    }

    public boolean isEmpty() {
        return collection.isEmpty();
    }

    public String getNextHref() {
        return TextUtils.isEmpty(next_href) ? null : next_href;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                '}';
    }

    public int size() {
        return collection != null ? collection.size() : 0;
    }

}

