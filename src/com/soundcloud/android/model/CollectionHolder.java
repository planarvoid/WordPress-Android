package com.soundcloud.android.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CollectionHolder<T> implements Iterable<T> {
    public ArrayList<T> collection;
    public String next_href;

    /** @noinspection unchecked*/
    @Override
    public Iterator<T> iterator() {
        return collection != null ?  collection.iterator() : (Iterator<T>) Collections.EMPTY_LIST.iterator();
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

