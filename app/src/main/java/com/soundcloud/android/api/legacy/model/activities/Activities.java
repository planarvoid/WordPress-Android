package com.soundcloud.android.api.legacy.model.activities;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.api.legacy.InvalidTokenException;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ErrorUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Activities extends CollectionHolder<Activity> {
    public static final int MAX_REQUESTS = 5;

    /* use this URL to poll for updates */
    @JsonProperty
    @JsonView(Views.Mini.class)
    public String future_href;

    public static final Activities EMPTY = new Activities();

    public Activities() {
        this.collection = new ArrayList<>();
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
        return !isEmpty() && collection.get(0).getCreatedAt().getTime() <= timestamp;
    }

    public boolean newerThan(long timestamp) {
        return !isEmpty() && collection.get(0).getCreatedAt().getTime() > timestamp;
    }

    public List<PublicApiUser> getUniqueUsers() {
        List<PublicApiUser> users = new ArrayList<>();
        for (Activity a : this) {
            if (a.getUser() != null && !users.contains(a.getUser())) {
                users.add(a.getUser());
            }
        }
        return users;
    }

    public List<Playable> getUniquePlayables() {
        List<Playable> playables = new ArrayList<>();
        for (Activity a : this) {
            if (a.getPlayable() != null && !playables.contains(a.getPlayable())) {
                playables.add(a.getPlayable());
            }
        }
        return playables;
    }

    public Activities selectType(Class<? extends Activity>... types) {
        List<Activity> activities = new ArrayList<>();
        for (Activity e : this) {
            for (Class<? extends Activity> type : types) {
                if (type.isAssignableFrom(e.getClass())) {
                    activities.add(e);
                }
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

    public Activities tracks() {
        return selectType(TrackActivity.class);
    }

    public Activities trackReposts() {
        return selectType(TrackRepostActivity.class);
    }

    public Activities followers() {
        return selectType(AffiliationActivity.class);
    }

    public void sort() {
        Collections.sort(collection);
    }

    @NotNull
    Activities merge(Activities old) {
        //noinspection ObjectEquality
        if (old == EMPTY) {
            return this;
        }

        Activities merged = new Activities(new ArrayList<>(collection));
        merged.future_href = future_href;
        merged.next_href = old.next_href;

        for (Activity a : old) {
            if (!merged.collection.contains(a)) {
                merged.collection.add(a);
            }
        }
        return merged;
    }


    @NonNull
    public Activities merge(Activities... activities) {
        Activities result = this;
        for (Activities activity : activities) {
            result = merge(activity);
        }
        return result;
    }

    public Activities filter(Date d) {
        return filter(d.getTime());
    }

    public Activities filter(long timestamp) {
        Iterator<Activity> it = collection.iterator();
        while (it.hasNext()) {
            if (it.next().getCreatedAt().getTime() <= timestamp) {
                it.remove();
            }
        }
        return this;
    }

    public long getTimestamp() {
        if (collection.isEmpty()) {
            return 0;
        } else {
            return collection.get(0).getCreatedAt().getTime();
        }
    }

    public static Activities fetchRecent(PublicApi api,
                                         final Request request,
                                         int max) throws IOException {

        return fetchRecent(api, request, max, 0);
    }

    private static Activities fetchRecent(PublicApi api,
                                          final Request request,
                                          int max,
                                          int requestNumber) throws IOException {
        if (max <= 0) {
            return EMPTY;
        }
        Request remote = new Request(request).set("limit", max);
        HttpResponse response = api.get(remote);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            Activities a = api.getMapper().readValue(response.getEntity().getContent(), Activities.class);
            if (a.size() < max && a.moreResourcesExist() && !a.isEmpty() && requestNumber < MAX_REQUESTS) {
                    /* should not happen in theory, but backend might limit max number per requests */
                return a.merge(fetchRecent(api, a.getNextRequest(), max - a.size(), requestNumber + 1));
            } else {
                return a;
            }
        } else {
            return handleUnexpectedResponse(remote, response);
        }
    }

    public static
    @Nullable
    Activities fetch(PublicApi api,
                     final Request request) throws IOException {
        HttpResponse response = api.get(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return api.getMapper().readValue(response.getEntity().getContent(), Activities.class);
        } else {
            return handleUnexpectedResponse(request, response);
        }
    }

    private static Activities handleUnexpectedResponse(Request request, HttpResponse response) throws IOException {
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_NO_CONTENT) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Got no content response (204)");
            }
            return EMPTY;
        } else if (PublicApi.isStatusCodeClientError(statusCode)) {
            // a 404 also translates to Unauthorized here, since the API is a bit fucked up
            throw new InvalidTokenException(response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());
        } else {
            final IOException ioException = new IOException(response.getStatusLine().toString());
            ErrorUtils.handleSilentException("Activities fetchRecent failed " + request, ioException);
            throw ioException;
        }
    }

    public ContentValues[] buildContentValues(final int contentId) {
        ContentValues[] cv = new ContentValues[size()];
        for (int i = 0; i < size(); i++) {
            cv[i] = get(i).buildContentValues();
            if (contentId >= 0) {
                cv[i].put(TableColumns.Activities.CONTENT_ID, contentId);
            }
        }
        return cv;
    }

    public String getFirstAvailableAvatar() {
        for (PublicApiUser u : getUniqueUsers()) {
            if (u.shouldLoadIcon()) {
                return u.avatar_url;
            }
        }
        return null;
    }

}
