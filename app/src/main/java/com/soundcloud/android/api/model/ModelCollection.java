package com.soundcloud.android.api.model;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModelCollection<T> implements Iterable<T> {

    public static final ModelCollection EMPTY = new ModelCollection<>(Collections.emptyList());

    @VisibleForTesting
    public static final String NEXT_LINK_REL = "next";

    protected final List<T> collection;
    protected final Map<String, Link> links;
    protected Urn queryUrn;

    public ModelCollection() {
        this(Collections.emptyList());
    }

    public ModelCollection(List<T> collection) {
        this.collection = collection;
        this.links = Collections.emptyMap();
    }

    public ModelCollection(List<T> collection,
                           Map<String, Link> links) {
        this.collection = collection;
        this.links = links == null ? Collections.emptyMap() : links;
    }

    public ModelCollection(List<T> collection, @Nullable String nextHref) {
        this.collection = collection;
        if (Strings.isNotBlank(nextHref)) {
            links = Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link(nextHref));
        } else {
            links = Collections.emptyMap();
        }
    }

    public ModelCollection(@JsonProperty("collection") List<T> collection,
                           @JsonProperty("_links") Map<String, Link> links,
                           @JsonProperty("query_urn") String queryUrn) {
        this(collection, links);
        if (queryUrn != null) {
            this.queryUrn = new Urn(queryUrn);
        }
    }

    public ModelCollection(List<T> collection, Map<String, Link> links, Urn queryUrn) {
        this(collection, links);
        if (queryUrn != null) {
            this.queryUrn = queryUrn;
        }
    }

    public ModelCollection<T> copyWithItems(List<T> items) {
        return new ModelCollection<T>(items, getLinks(), queryUrn);
    }

    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    public List<T> getCollection() {
        return collection;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    @JsonIgnore
    public Optional<Link> getNextLink() {
        return Optional.fromNullable(links.get(NEXT_LINK_REL));
    }

    @JsonIgnore
    public Optional<Urn> getQueryUrn() {
        return Optional.fromNullable(queryUrn);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(o instanceof ModelCollection)) {
            return false;
        }

        ModelCollection<?> that = (ModelCollection<?>) o;
        return MoreObjects.equal(collection, that.collection) &&
                MoreObjects.equal(links, that.links) &&
                MoreObjects.equal(queryUrn, that.queryUrn);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(collection, links, queryUrn);
    }

    public <S> ModelCollection<S> transform(Function<T, S> function) {
        return new ModelCollection<>(newArrayList(Iterables.transform(collection, function)), links, queryUrn);
    }
}
