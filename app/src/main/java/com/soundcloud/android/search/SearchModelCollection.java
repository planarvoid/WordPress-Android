package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.of;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

class SearchModelCollection<T> extends ModelCollection<T> {

    private static final String PROPERTY_PREMIUM_CONTENT = "premium_content";

    private final Optional<ModelCollection<T>> premiumContent;

    SearchModelCollection(List<T> collection) {
        super(collection);
        premiumContent = Optional.absent();
    }

    SearchModelCollection(List<T> collection, Map<String, Link> links) {
        super(collection, links);
        premiumContent = Optional.absent();
    }

    SearchModelCollection(@JsonProperty(PROPERTY_COLLECTION) List<T> collection,
                          @JsonProperty(PROPERTY_LINKS) Map<String, Link> links,
                          @JsonProperty(PROPERTY_QUERY_URN) String queryUrn,
                          @JsonProperty(PROPERTY_PREMIUM_CONTENT) @Nullable ModelCollection<T> premiumContent) {
        super(collection, links, queryUrn);
        if (premiumContent == null || premiumContent.getCollection().isEmpty()) {
            this.premiumContent = Optional.absent();
        } else {
            this.premiumContent = of(premiumContent);
        }
    }

    Optional<ModelCollection<T>> premiumContent() {
        return premiumContent;
    }
}
