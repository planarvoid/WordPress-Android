package com.soundcloud.android.api.legacy.model;

import java.util.List;

public class SearchResultsCollection extends PublicApiResource.ResourceHolder<PublicApiResource> {

    @SuppressWarnings("unused") // Jackson calls this
    public SearchResultsCollection() { }

    public SearchResultsCollection(List<PublicApiResource> collection) {
        super(collection);
    }

    public SearchResultsCollection(List<PublicApiResource> results, String nextHref){
        super(results, nextHref);
    }
}
