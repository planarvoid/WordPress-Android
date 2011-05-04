
package com.soundcloud.android.objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities implements Iterable<Event> {
    public ArrayList<Event> collection;
    public String next_href;

    @Override
    public Iterator<Event> iterator() {
        return collection.iterator();
    }

    public void setCursorToLastEvent(){

        Collections.sort(collection, new Event.CompareCreatedAt());

        for (Event e : collection){
            Log.i("EventsAdapter","Added Track "  +e.created_at + " " + e.created_at.getTime()+ " " + e.getTrack().id);
        }

        int cursorStart = next_href.indexOf("cursor=") + 7;
        if (cursorStart > -1){
            Log.i("EventsAdapter","Setting cursor to " + next_href.substring(cursorStart,next_href.substring(cursorStart).indexOf("&")+cursorStart));
            collection.get(collection.size()-1).next_cursor = next_href.substring(cursorStart,next_href.substring(cursorStart).indexOf("&")+cursorStart);
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
}
