package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModelCollection<T> implements Iterable<T> {

    public static final String NEXT_LINK_REL = "next";

    private List<T> mCollection = Collections.emptyList();
    private Map<String, Link> mLinks = Collections.emptyMap();

    public ModelCollection(List<T> collection) {
        mCollection = collection;
    }

    public ModelCollection() {
        /* deserialization */
    }

    @Override
    public Iterator<T> iterator() {
        return mCollection.iterator();
    }

    public void setCollection(List<T> collection) {
        this.mCollection = collection;
    }

    public List<T> getCollection() {
        return mCollection;
    }

    public Map<String, Link> getLinks() {
        return mLinks;
    }

    @JsonProperty("_links")
    public void setLinks(Map<String, Link> mLinks) {
        this.mLinks = mLinks;
    }

    public Optional<Link> getNextLink() {
        return Optional.fromNullable(mLinks.get(NEXT_LINK_REL));
    }
}
