package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.Request;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jetbrains.annotations.Nullable;

import android.text.TextUtils;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * Holder for data returned in the API's "linked_partitioning" format (/tracks?linked_partitioning=1)
 *
 * @param <T>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionHolder<T> implements Iterable<T> {
    private static final Predicate UNKNOWN_RESOURCE_PREDICATE = new Predicate() {
        @Override
        public boolean apply(@Nullable Object resource) {
            return resource instanceof UnknownResource;
        }
    };

    @JsonProperty
    @JsonView(Views.Mini.class)
    public List<T> collection;

    @JsonProperty @JsonView(Views.Mini.class)
    public String next_href;

    public CollectionHolder() {
        this(Collections.<T>emptyList());
    }

    public CollectionHolder(List<T> collection){
        this.collection = Lists.newArrayList(collection);
    }

    public CollectionHolder(List<T> collection, String nextHref){
        this(Lists.newArrayList(collection));
        next_href = nextHref;
    }

    /** @noinspection unchecked*/
    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    public boolean add(T item) {
        return collection.add(item);
    }

    public T get(int index) {
        return collection.get(index);
    }

    public List<T> getCollection(){
        return Collections.unmodifiableList(collection);
    }

    public boolean moreResourcesExist() {
        return !TextUtils.isEmpty(next_href);
    }

    public Request getNextRequest() {
        if (!moreResourcesExist()) {
            throw new IllegalStateException("next_href is null");
        } else {
            return new Request(URI.create(next_href));
        }
    }

    public boolean isEmpty() {
        return collection.isEmpty();
    }

    public String getCursor() {
        if (next_href != null) {
            List<NameValuePair> params = URLEncodedUtils.parse(URI.create(next_href), "UTF-8");
            for (NameValuePair param : params) {
                if (param.getName().equalsIgnoreCase("cursor")) {
                    return param.getValue();
                }
            }
        }
        return null;
    }

    public void removeUnknownResources(){
        Collection<T> unknownResources = Collections2.filter(collection, UNKNOWN_RESOURCE_PREDICATE);
        collection.removeAll(unknownResources);
    }

    public String getNextHref(){
        return TextUtils.isEmpty(next_href) ? null : next_href;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                '}';
    }

    public int size() {
        return collection != null ? collection.size() : 0;
    }

}

