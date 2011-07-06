package com.soundcloud.android.model;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.task.LoadJsonTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.text.TextUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CollectionHolder<T> implements Iterable<T> {
    public ArrayList<T> collection;
    public String next_href;

    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                '}';
    }

    public int size() {
        return collection.size();
    }

    public String getCursor() {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(next_href), "UTF-8");
        for (NameValuePair param : params) {
            if (param.getName().equalsIgnoreCase("cursor")) {
                return param.getValue();
            }
        }
        return null;
    }
}

