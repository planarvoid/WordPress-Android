package com.soundcloud.android.model;

import android.content.Context;
import android.text.TextUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.json.Views;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.soundcloud.android.model.ScModelManager.validateResponse;


/**
 * Holder for data returned in the API's "linked_partitioning" format (/tracks?linked_partitioning=1)
 *
 * @param <T>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionHolder<T> implements List<T> {
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

    @Override
    public int lastIndexOf(Object object) {
        return collection.lastIndexOf(object);
    }

    @Override
    public ListIterator<T> listIterator() {
        return collection.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        return collection.listIterator(location);
    }

    @Override
    public T remove(int location) {
        return collection.remove(location);
    }

    @Override
    public boolean remove(Object object) {
        return collection.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> thatCollection) {
        return collection.removeAll(thatCollection);
    }

    @Override
    public boolean retainAll(Collection<?> thatCollection) {
        return collection.retainAll(thatCollection);
    }

    @Override
    public T set(int location, T object) {
        return collection.set(location, object);
    }

    public T get(int index) {
        return collection.get(index);
    }

    @Override
    public int indexOf(Object object) {
        return collection.indexOf(object);
    }

    @Override
    public void add(int location, T object) {
        collection.add(location, object);
    }

    public boolean add(T e) {
        return collection.add(e);
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> thatCollection) {
        return collection.addAll(location, thatCollection);
    }

    @Override
    public boolean addAll(Collection<? extends T> thatCollection) {
        return collection.addAll(thatCollection);
    }

    @Override
    public void clear() {
        collection.clear();
    }

    @Override
    public boolean contains(Object object) {
        return collection.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> thatCollection) {
        return collection.containsAll(thatCollection);
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

    @Override
    public List<T> subList(int start, int end) {
        return collection.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <T1 extends Object> T1[] toArray(T1[] array) {
        return collection.toArray(array);
    }

    public void resolve(Context context) {
        for (T m : this) {
            if (m instanceof ScModel) {
                ((ScModel)m).resolve(context);
            }
        }
    }

    public @NotNull static <T, C extends CollectionHolder<T>> List<T> fetchAllResources(AndroidCloudAPI api,
                                                                                        Request request,
                                                                                        Class<C> ch) throws IOException {
        List<T> objects = new ArrayList<T>();
        C holder = null;
        do {
            Request r = holder == null ? request : Request.to(holder.next_href);
            HttpResponse resp = validateResponse(api.get(r.with(LINKED_PARTITIONING, "1")));
            holder = api.getMapper().readValue(resp.getEntity().getContent(), ch);
            if (holder == null) throw new IOException("invalid data");

            if (holder.collection != null) {
                objects.addAll(holder.collection);
            }
        } while (holder.next_href != null);

        return objects;
    }
}

