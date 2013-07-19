package com.soundcloud.android.model.act;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Activities extends CollectionHolder<Activity> {
    public static final int MAX_REQUESTS = 5;

    /* use this URL to poll for updates */
    @JsonProperty
    @JsonView(Views.Mini.class)
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


    @SuppressWarnings("UnusedDeclaration")
    public boolean olderThan(long timestamp) {
        return !isEmpty() && collection.get(0).created_at.getTime() <= timestamp;
    }

    public boolean newerThan(long timestamp) {
        return !isEmpty() && collection.get(0).created_at.getTime() > timestamp;
    }

    public List<User> getUniqueUsers() {
        List<User> users = new ArrayList<User>();
        for (Activity a : this) {
            if (a.getUser() != null && !users.contains(a.getUser())) {
                users.add(a.getUser());
            }
        }
        return users;
    }

    public List<Playable> getUniquePlayables() {
        List<Playable> playables = new ArrayList<Playable>();
        for (Activity a : this) {
            if (a.getPlayable() != null && !playables.contains(a.getPlayable())) {
                playables.add(a.getPlayable());
            }
        }
        return playables;
    }

    public Activities selectType(Class<? extends Activity>... types) {
        List<Activity> activities = new ArrayList<Activity>();
        for (Activity e : this) {
            for (Class<? extends Activity> type : types)
            if (type.isAssignableFrom(e.getClass())) {
                activities.add(e);
            }
        }
        return new Activities(activities);
    }

    public Activities trackLikes() {
        return selectType(TrackLikeActivity.class);
    }

    public Activities comments() {
        return selectType(CommentActivity.class);
    }

    public Activities sharings() {
        return selectType(TrackSharingActivity.class);
    }

    public Activities tracks() {
        return selectType(TrackActivity.class);
    }

    public Activities trackReposts() {
        return selectType(TrackRepostActivity.class);
    }

    public Map<Playable, Activities> groupedByPlayable() {
        Map<Playable,Activities> grouped = new HashMap<Playable, Activities>();

        for (Activity e : this) {
            Activities activities = grouped.get(e.getPlayable());
            if (activities == null) {
                activities = new Activities();
                grouped.put(e.getPlayable(), activities);
            }
            activities.add(e);
        }
        return grouped;
    }

    public void sort() {
        Collections.sort(collection);
    }

    public @NotNull Activities merge(Activities old) {
        //noinspection ObjectEquality
        if (old == EMPTY) return this;

        Activities merged = new Activities(new ArrayList<Activity>(collection));
        merged.future_href = future_href;
        merged.next_href = old.next_href;

        for (Activity a : old) {
            if (!merged.collection.contains(a)) {
                merged.collection.add(a);
            }
        }
        return merged;
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
                if (e.getPlayable() != null) ids.add(e.getPlayable().getId());
            }
        }
        return ids.size();
    }

    public static Activities fetchRecent(AndroidCloudAPI api,
                                         final Request request,
                                         int max) throws IOException {

        return fetchRecent(api, request, max, 0);
    }

    private static Activities fetchRecent(AndroidCloudAPI api,
                                         final Request request,
                                         int max,
                                         int requestNumber) throws IOException {
        if (max <= 0) return EMPTY;
        Request remote = new Request(request).set("limit", max);
        HttpResponse response = api.get(remote);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            Activities a = api.getMapper().readValue(response.getEntity().getContent(), Activities.class);
            if (a.size() < max && a.moreResourcesExist() && !a.isEmpty() && requestNumber < MAX_REQUESTS) {
                    /* should not happen in theory, but backend might limit max number per requests */
                return a.merge(fetchRecent(api, a.getNextRequest(), max - a.size(), requestNumber+1));
            } else {
                return a;
            }
        } else {
            return handleUnexpectedResponse(response);
        }
    }

    public static @Nullable Activities fetch(AndroidCloudAPI api,
                                   final Request request) throws IOException {
        HttpResponse response = api.get(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return api.getMapper().readValue(response.getEntity().getContent(), Activities.class);
        } else {
            return handleUnexpectedResponse(response);
        }
    }

    private static Activities handleUnexpectedResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Got no content response (204)");
            return EMPTY;
        } else  if (Wrapper.isStatusCodeClientError(response.getStatusLine().getStatusCode())){
            throw new CloudAPI.InvalidTokenException(response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());
        } else {
            throw new IOException(response.getStatusLine().toString());
        }
    }

    public ContentValues[] buildContentValues(final int contentId) {
        ContentValues[] cv = new ContentValues[size()];
        for (int i=0; i<size(); i++) {
            cv[i] = get(i).buildContentValues();
            if (contentId >= 0) {
                cv[i].put(DBHelper.Activities.CONTENT_ID, contentId);
            }
        }
        return cv;
    }

    public Set<String> artworkUrls() {
        Set<String> artworkUrls = new HashSet<String>();
        for (Activity a : this) {
            Playable playable = a.getPlayable();
            if (playable != null) {
                String artworkUrl = playable.getArtwork();
                if (!TextUtils.isEmpty(artworkUrl)) {
                    artworkUrls.add(artworkUrl);
                }
            }
        }
        return artworkUrls;
    }

    public String getFirstAvailableAvatar() {
        for (User u : getUniqueUsers()) {
            if (u.shouldLoadIcon()) {
                return u.avatar_url;
            }
        }
        return null;
    }

    public String getFirstAvailableArtwork() {
        for (Activity a : this) {
            Playable p = a.getPlayable();
            if (p != null && p.shouldLoadIcon()) {
                return p.artwork_url;
            }
        }
        // no artwork found, fall back to avatar
        return getFirstAvailableAvatar();
    }
}
