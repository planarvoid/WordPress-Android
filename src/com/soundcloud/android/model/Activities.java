package com.soundcloud.android.model;

import android.util.Log;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.text.TextUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities implements Iterable<Event> {
    public ArrayList<Event> collection;
    public String next_href;

    @Override
    public Iterator<Event> iterator() {
        return collection.iterator();
    }

    public void setCursorToLastEvent() {
        int cursorStart = TextUtils.isEmpty(next_href) ? -1 : next_href.indexOf("cursor=") + 7;
        if (cursorStart > -1) {
            collection.get(collection.size() - 1).next_cursor = next_href.substring(cursorStart, next_href.substring(cursorStart).indexOf("&") + cursorStart);
        }
    }

    @Override
    public String toString() {
        return "Activities{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                '}';
    }

    public int size() {
        return collection.size();
    }

    public String getCursor() {
        Log.i("asdf", "Get Cursor " + next_href);
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(next_href), "UTF-8");
        for (NameValuePair param : params) {
            if (param.getName().equalsIgnoreCase("cursor")) {
                return param.getValue();
            }
        }
        return null;
    }
}
