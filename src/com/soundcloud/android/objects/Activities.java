
package com.soundcloud.android.objects;

import java.util.ArrayList;
import java.util.Iterator;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities implements Iterable<Event> {
    public ArrayList<Event> collection;
    public String next_href;

    @Override
    public Iterator<Event> iterator() {
        return collection.iterator();
    }

    @Override
    public String toString() {
        return "Activities{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                '}';
    }
}
