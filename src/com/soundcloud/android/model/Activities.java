package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import android.accounts.Account;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Activities extends CollectionHolder<Activity> {
    /* use this URL to poll for updates */
    @JsonProperty @JsonView(Views.Mini.class)
    public String future_href;

    public static final Activities EMPTY = new Activities();

    public Activities() {
        this.collection =  new ArrayList<Activity>();
    }

    public Activities(List<Activity> collection) {
        this.collection = collection;
    }

    public Activities(Activity... activities) {
        this.collection = Arrays.asList(activities);
    }

    public Activities(List<Activity> collection, String future_href, String next_href) {
        this.collection = collection;
        this.future_href = future_href;
        this.next_href = next_href;
    }

    @Override
    public String toString() {
        return "Activities{" +
                "collection=" + collection +
                ", next_href='" + next_href + '\'' +
                ", future_href='" + future_href + '\'' +
                '}';
    }


    public boolean olderThan(long timestamp) {
        return !isEmpty() && collection.get(0).created_at.getTime() <= timestamp;
    }

    public boolean newerThan(long timestamp) {
        return !isEmpty() && collection.get(0).created_at.getTime() > timestamp;
    }

    public List<User> getUniqueUsers() {
        List<User> users = new ArrayList<User>();
        for (Activity e : this) {
            if (e.getUser() != null && !users.contains(e.getUser())) {
                users.add(e.getUser());
            }
        }
        return users;
    }

    public List<Track> getUniqueTracks() {
        List<Track> tracks = new ArrayList<Track>();
        for (Activity e : this) {
            if (e.getTrack() != null && !tracks.contains(e.getTrack())) {
                tracks.add(e.getTrack());
            }
        }
        return tracks;
    }

    public Activities selectType(String type) {
        List<Activity> activities = new ArrayList<Activity>();
        for (Activity e : this) {
            if (type.equals(e.type)) {
                activities.add(e);
            }
        }
        return new Activities(activities);
    }

    public Activities favoritings() {
        return selectType(Activity.Types.FAVORITING);
    }

    public Activities comments() {
        return selectType(Activity.Types.COMMENT);
    }

    public Activities sharings() {
        return selectType(Activity.Types.TRACK_SHARING);
    }

    public Activities tracks() {
        return selectType(Activity.Types.TRACK);
    }

    public Map<Track, Activities> groupedByTrack() {
        Map<Track,Activities> grouped = new HashMap<Track, Activities>();

        for (Activity e : this) {
            Activities evts = grouped.get(e.getTrack());
            if (evts == null) {
                evts = new Activities();
                grouped.put(e.getTrack(), evts);
            }
            evts.add(e);
        }
        return grouped;
    }


    public static Activities fromJSON(InputStream is) throws IOException {
        return AndroidCloudAPI.Mapper.readValue(is, Activities.class);
    }

    public static Activities fromJSON(File f) throws IOException {
        return AndroidCloudAPI.Mapper.readValue(f, Activities.class);
    }

    public static Activities fromJSON(String is) throws IOException {
        return AndroidCloudAPI.Mapper.readValue(is, Activities.class);
    }

    public String toJSON() throws IOException {
        return toJSON(Views.Mini.class);
    }

    public String toJSON(Class<?> view) throws IOException {
        return AndroidCloudAPI.Mapper.viewWriter(view).writeValueAsString(this);
    }

    public Activities toJSON(File f, Class<?> view) throws IOException {
        AndroidCloudAPI.Mapper.viewWriter(Views.Mini.class).writeValue(f, this);
        return this;
    }

    public Activities merge(Activities old) {
        Activities merged = new Activities(new ArrayList<Activity>(collection));
        merged.future_href = future_href;
        merged.next_href = old.next_href;

        Activity last = lastEvent();
        if (last != null) last.next_href = next_href;

        for (Activity e : old) {
            if (!merged.collection.contains(e)) {
                merged.collection.add(e);
            }
        }
        return merged;
    }

    private Activity lastEvent() {
        return isEmpty() ? null : collection.get(collection.size()-1);
    }

    public Activities returnUnique(Activities old) {
        Activities unique = new Activities(new ArrayList<Activity>());
        unique.future_href = future_href;

        for (Activity e : collection) {
            if (!old.collection.contains(e)) {
                unique.collection.add(e);
            }
        }
        return unique;
    }

    public Activities filter(Date d) {
        return filter(d.getTime());
    }

    public Activities filter(long timestamp) {
        Iterator<Activity> it = collection.iterator();
        while (it.hasNext()) {
            if (it.next().created_at.getTime() <= timestamp) it.remove();
        }
        return this;
    }

    public Activities trimBelow(int max) {
        if (collection.size() <= max) return this;
        int i = max;
        while (i > 0 && collection.get(i-1).next_href == null){ i--; }


        Activities trimmed = new Activities(new ArrayList<Activity>(collection.subList(0, i)));
        trimmed.next_href = trimmed.isEmpty() ? null : trimmed.lastEvent().next_href;
        trimmed.future_href = future_href;
        return trimmed;
    }

    public long getTimestamp() {
        if (collection.isEmpty()) {
            return 0;
        } else {
            return collection.get(0).created_at.getTime();
        }
    }

    public static int getUniqueTrackCount(Activities... activities) {
        Set<Long> ids = new HashSet<Long>(10);
        for (Activities a : activities) {
            for (Activity e : a) {
                if (e.getTrack() != null) ids.add(e.getTrack().id);
            }
        }
        return ids.size();
    }

    public static Activities fetch(AndroidCloudAPI api,
                                   final Request request,
                                   final Activity lastCached,
                                   int max)
            throws IOException {
        boolean caughtUp = false;
        String future_href = null;
        String next_href = null;
        List<Activity> activityList = new ArrayList<Activity>();

        
        Request remote = new Request(request).add("limit", 20);
        do {
            Log.d(TAG, "Making request " + remote);
            HttpResponse response = api.get(remote);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Activities activities = fromJSON(response.getEntity().getContent());
                remote = activities.hasMore() ? activities.getNextRequest() : null;
                if (future_href == null) {
                    future_href = URLDecoder.decode(activities.future_href);
                }

                if (next_href != null) {
                    activityList.get(activityList.size()-1).next_href = next_href;
                }
                next_href = activities.next_href;

                Log.d(TAG,"Got events " + activities.size());

                for (Activity evt : activities) {
                    if ((lastCached == null || !evt.equals(lastCached)) &&
                        (max < 0 ||  activityList.size() < max)) {
                        activityList.add(evt);
                    } else {
                        caughtUp = true;
                        break;
                    }
                }
            } else {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    return EMPTY;
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                            response.getStatusLine().getReasonPhrase());
                } else {
                    throw new IOException(response.getStatusLine().toString());
                }
            }
        } while (!caughtUp
                && remote != null);

        return new Activities(activityList, future_href, next_href);
    }

    public static void clear(Context c) {
    }


    public static Activities get(SoundCloudApplication context,
                                 Account account,
                                 final Request request) throws IOException {


//        Activities activities;
//        try {
//            if (cachedFile.exists()) {
//                Activities cached = fromJSON(cachedFile);
//                String future_href = cached.future_href;
//
//                Log.d(TAG, "future_href href is " + future_href);
//                Log.d(TAG, "read from activities cache "+cachedFile+
//                        ", requesting updates from " +(future_href == null ? request.toUrl() : future_href));
//
//                if (future_href != null) {
//                    Activities updates = fetch(context, cached.size() > 0 ? cached.get(0) : null, Request.to(future_href));
//                    activities = updates == EMPTY ? cached : updates.merge(cached);
//                } else {
//                    activities = cached;
//                }
//            } else {
//              activities = fetch(context, null, request);
//            }
//        } catch (IOException e) {
//            Log.w(TAG, "error", e);
//            // fallback, load events from normal resource
//            activities = fetch(context, null, request);
//        }
//
//        Log.d(TAG, "caching activities to "+cachedFile);
//        return activities.trimBelow(SyncAdapterService.NOTIFICATION_MAX)
//                         .toJSON(cachedFile, Views.Mini.class);
        return null;
    }

}
