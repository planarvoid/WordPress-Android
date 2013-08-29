package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelCollection<T> {

    public static final String NEXT_LINK_REL = "next";
    public static final String SELF_LINK_REL = "self";

    private List<T> mCollection = Collections.emptyList();
    private Map<String, Link> mLinks = Collections.emptyMap();

    public List<T> getCollection() {
        return mCollection;
    }

    public void setCollection(List<T> collection) {
        this.mCollection = collection;
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

    public Link getSelfLink() {
        return mLinks.get(SELF_LINK_REL);
    }
}
