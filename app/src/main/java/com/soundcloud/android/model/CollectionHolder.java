package com.soundcloud.android.model;

import static com.soundcloud.android.model.ScModelManager.validateResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.json.Views;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
    public static final String LINKED_PARTITIONING = "linked_partitioning";

    @JsonProperty
    @JsonView(Views.Mini.class)
    public List<T> collection;

    @JsonProperty @JsonView(Views.Mini.class)
    public String next_href;

    public CollectionHolder() {
        this(Collections.<T>emptyList());
    }

    public CollectionHolder(List<T> collection){
        this.collection = collection;
    }

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

    public void resolve(Context context) {
        for (T m : this) {
            if (m instanceof ScModel) {
                ((ScModel)m).resolve(context);
            }
        }
    }

    public static <T> List<T> fetchAllResources(AndroidCloudAPI api,
                                                Request request,
                                                Class<? extends CollectionHolder<T>> ch) throws IOException {
        List<T> objects = new ArrayList<T>();
        CollectionHolder<T> holder = null;
        do {
            Request r =  holder == null ? request : Request.to(holder.next_href);
            HttpResponse resp = validateResponse(api.get(r.with(LINKED_PARTITIONING, "1")));
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                holder = api.getMapper().readValue(resp.getEntity().getContent(), ch);
                if (holder.collection != null) {
                    objects.addAll(holder.collection);
                }
            }
        } while (holder != null && holder.next_href != null);
        return objects;
    }
}

