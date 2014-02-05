package com.soundcloud.android.model;

import java.util.List;

public class SearchResultsCollection extends ModelCollection<ScResource> {

    @SuppressWarnings("unused") // Jackson calls this
    public SearchResultsCollection() {
    }

    public SearchResultsCollection(List<ScResource> collection) {
        super(collection);
    }

}
