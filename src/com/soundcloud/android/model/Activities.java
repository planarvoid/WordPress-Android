package com.soundcloud.android.model;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.json.Views;
import com.soundcloud.api.Request;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Activities implements Iterable<Event> {
    @JsonProperty @JsonView(Views.Mini.class)
    public List<Event> collection;

    /* the next page for the collection */
    @JsonProperty @JsonView(Views.Mini.class)
    public String next_href;

    /* use this URL to poll for updates */
    @JsonProperty @JsonView(Views.Mini.class)
    public String future_href;

    public static final Activities EMPTY = new Activities();

    public Activities() {
        this.collection =  new ArrayList<Event>();
    }

    public Activities(List<Event> collection) {
        this.collection = collection;
    }

    public Activities(List<Event> collection, String future_href) {
        this.collection = collection;
        this.future_href = future_href;
    }

    public Activities(Event... events) {
        this.collection = Arrays.asList(events);
    }

    @Override
    public Iterator<Event> iterator() {
        return collection.iterator();
    }

    @Override
    public String toString() {
        return "Activities{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                ", future_href='" + future_href + '\'' +
                '}';
    }

    public int size() {
        return collection.size();
    }

    public Event get(int index) {
        return collection.get(index);
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

    public boolean includes(long timestamp) {
        return !isEmpty() && collection.get(0).created_at.getTime() <= timestamp;
    }

    public List<User> getUniqueUsers() {
        List<User> users = new ArrayList<User>();
        for (Event e : this) {
            if (e.getUser() != null && !users.contains(e.getUser())) {
                users.add(e.getUser());
            }
        }
        return users;
    }

    public List<Track> getUniqueTracks() {
        List<Track> tracks = new ArrayList<Track>();
        for (Event e : this) {
            if (e.getTrack() != null && !tracks.contains(e.getTrack())) {
                tracks.add(e.getTrack());
            }
        }
        return tracks;
    }

    public Activities selectType(String type) {
        List<Event> events = new ArrayList<Event>();
        for (Event e : this) {
            if (type.equals(e.type)) {
                events.add(e);
            }
        }
        return new Activities(events);
    }

    public Activities favoritings() {
        return selectType(Event.Types.FAVORITING);
    }

    public Activities comments() {
        return selectType(Event.Types.COMMENT);
    }

    public Activities sharings() {
        return selectType(Event.Types.TRACK_SHARING);
    }

    public Activities tracks() {
        return selectType(Event.Types.TRACK);
    }

    public Map<Track, Activities> groupedByTrack() {
        Map<Track,Activities> grouped = new HashMap<Track, Activities>();

        for (Event e : this) {
            Activities evts = grouped.get(e.getTrack());
            if (evts == null) {
                evts = new Activities();
                grouped.put(e.getTrack(), evts);
            }
            evts.add(e);
        }
        return grouped;
    }

    private void add(Event e) {
        collection.add(e);
    }

    // used for testing
    /* package */ static Activities fromJSON(InputStream is) throws IOException {
        return AndroidCloudAPI.Mapper.readValue(is, Activities.class);
    }

    /* package */ static Activities fromJSON(String is) throws IOException {
        return AndroidCloudAPI.Mapper.readValue(is, Activities.class);
    }

    public String toJSON() throws IOException {
        return AndroidCloudAPI.Mapper.viewWriter(Views.Mini.class).writeValueAsString(this);
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
}
