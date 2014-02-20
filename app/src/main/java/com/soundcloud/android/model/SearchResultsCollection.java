package com.soundcloud.android.model;

import java.util.List;

public class SearchResultsCollection extends ScResource.ScResourceHolder<ScResource> {

    @SuppressWarnings("unused") // Jackson calls this
    public SearchResultsCollection() { }

    public SearchResultsCollection(List<ScResource> collection) {
        super(collection);
    }

    public SearchResultsCollection(List<ScResource> results, String nextHref){
        super(results, nextHref);
    }
}
