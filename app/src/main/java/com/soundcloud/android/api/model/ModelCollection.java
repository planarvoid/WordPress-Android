package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModelCollection<T> implements Iterable<T> {

    public static final String NEXT_LINK_REL = "next";

    private List<T> collection = Collections.emptyList();
    private Map<String, Link> links = Collections.emptyMap();

    public ModelCollection(List<T> collection) {
        this.collection = collection;
    }

    public ModelCollection(List<T> collection, Map<String, Link> links) {
        this(collection);
        this.links = links;
    }

    public ModelCollection(List<T> collection, @Nullable String nextHref) {
        this(collection);
        if (Strings.isNotBlank(nextHref)) {
            links = Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link(nextHref));
        }
    }

    public ModelCollection() {
        /* deserialization */
    }

    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    public void setCollection(List<T> collection) {
        this.collection = collection;
    }

    public List<T> getCollection() {
        return collection;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    @JsonProperty("_links")
    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public Optional<Link> getNextLink() {
        return Optional.fromNullable(links.get(NEXT_LINK_REL));
    }
}
