package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SearchCollection<T> implements Iterable<T> {
    public static final String NEXT_LINK_REL = "next";
    private Urn queryUrn;
    private List<T> collection = Collections.emptyList();
    private Map<String, Link> links = Collections.emptyMap();

    public SearchCollection(List<T> collection) {
        this.collection = collection;
    }

    public SearchCollection() {
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
    public void setLinks(Map<String, Link> mLinks) {
        this.links = mLinks;
    }

    public Optional<Link> getNextLink() {
        return Optional.fromNullable(links.get(NEXT_LINK_REL));
    }

    @JsonProperty("query_urn")
    public void setQueryUrn(String queryUrn) {
        this.queryUrn = new Urn(queryUrn);
    }

    public Optional<Urn> getQueryUrn() {
        return Optional.fromNullable(queryUrn);
    }
}
