package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;
import com.soundcloud.api.Request;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.text.TextUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CollectionHolder<T> implements Iterable<T> {
    @JsonProperty
    @JsonView(Views.Mini.class)
    public List<T> collection;

    @JsonProperty @JsonView(Views.Mini.class)
    public String next_href;

    /** @noinspection unchecked*/
    @Override
    public Iterator<T> iterator() {
        return collection != null ?  collection.iterator() : (Iterator<T>) Collections.EMPTY_LIST.iterator();
    }

    public T get(int index) {
        return collection.get(index);
    }

    protected void add(T e) {
        collection.add(e);
    }

    public boolean hasMore() {
        return !TextUtils.isEmpty(next_href);
    }

    public Request getNextRequest() {
        if (!hasMore()) {
            throw new IllegalStateException("next_href is null");
        } else {
            return new Request(URI.create(next_href));
        }
    }

    public boolean isEmpty() {
        return size() == 0;
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

