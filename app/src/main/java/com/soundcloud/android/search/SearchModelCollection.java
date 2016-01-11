package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.absent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

class SearchModelCollection<T> extends ModelCollection<T> {

    private final Optional<ModelCollection<T>> premiumContent;

    SearchModelCollection(List<T> collection) {
        super(collection);
        premiumContent = absent();
    }

    SearchModelCollection(List<T> collection, Map<String, Link> links) {
        super(collection, links);
        premiumContent = absent();
    }

    SearchModelCollection(@JsonProperty("collection") List<T> collection,
                          @JsonProperty("_links") Map<String, Link> links,
                          @JsonProperty("query_urn") String queryUrn,
                          @JsonProperty("premium_content") @Nullable ModelCollection<T> premiumContent) {
        super(collection, links, queryUrn);
        if (premiumContent == null || premiumContent.getCollection().isEmpty()) {
            this.premiumContent = absent();
        } else {
            this.premiumContent = Optional.of(premiumContent);
        }
    }

    Optional<ModelCollection<T>> premiumContent() {
        return premiumContent;
    }
}
