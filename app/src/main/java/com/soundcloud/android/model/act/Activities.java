package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.*;

import static com.soundcloud.android.SoundCloudApplication.*;

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

    public List<Track> getUniqueTracks() {
        List<Track> tracks = new ArrayList<Track>();
        for (Activity a : this) {
            if (a.getTrack() != null && !tracks.contains(a.getTrack())) {
                tracks.add(a.getTrack());
            }
        }
        return tracks;
    }

    public List<Comment> getUniqueComments() {
            List<Comment> tracks = new ArrayList<Comment>();
            for (Activity a : this) {
                if (a instanceof CommentActivity && ((CommentActivity)a).comment != null && !tracks.contains(((CommentActivity)a).comment)) {
                    tracks.add(((CommentActivity)a).comment);
                }
            }
            return tracks;
        }

    public Activities selectType(Class type) {
        List<Activity> activities = new ArrayList<Activity>();
        for (Activity e : this) {
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


    // TODO, get rid of future href and next href and generate them
    public Activities merge(Activities old) {
        //noinspection ObjectEquality
        if (old == EMPTY) return this;

        Activities merged = new Activities(new ArrayList<Activity>(collection));
        merged.future_href = future_href;
        merged.next_href = old.next_href;

        for (Activity e : old) {
            if (!merged.collection.contains(e)) {
                merged.collection.add(e);
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

    public long getLastTimestamp() {
        if (collection.isEmpty()) {
            return 0;
        } else {
            return collection.get(collection.size()-1).created_at.getTime();
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
        final int status = response.getStatusLine().getStatusCode();
        switch (status) {
            case HttpStatus.SC_OK: {
                Activities a = SoundCloudApplication.MODEL_MANAGER.fromJSON(response.getEntity().getContent());
                if (a.size() < max && a.hasMore() && !a.isEmpty() && requestNumber < MAX_REQUESTS) {
                    /* should not happen in theory, but backend might limit max number per requests */
                    return a.merge(fetchRecent(api, a.getNextRequest(), max - a.size(), requestNumber+1));
                } else {
                    return a;
                }
            }
            case HttpStatus.SC_NO_CONTENT: {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Got no content response (204)");
                return EMPTY;
            }
            case HttpStatus.SC_UNAUTHORIZED:
                throw new InvalidTokenException(status, response.getStatusLine().getReasonPhrase());

            // sync will get retried later
            default: throw new IOException(response.getStatusLine().toString());
        }
    }

    public static Activities fetch(AndroidCloudAPI api,
                                   final Request request) throws IOException {
        HttpResponse response = api.get(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return SoundCloudApplication.MODEL_MANAGER.fromJSON(response.getEntity().getContent());
        } else {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Got no content response (204)");
                return EMPTY;
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                        response.getStatusLine().getReasonPhrase());
            } else {
                throw new IOException(response.getStatusLine().toString());
            }
        }
    }

    public static int clear(@Nullable Content content, ContentResolver resolver) {
        Content contentToDelete = Content.ME_ALL_ACTIVITIES;
        if (content != null) {
            contentToDelete = content;
        }
        if (!Activity.class.isAssignableFrom(contentToDelete.modelType)) {
            throw new IllegalArgumentException("specified content is not an activity");
        }
        // make sure to delete corresponding collection
        if (contentToDelete == Content.ME_ALL_ACTIVITIES) {
            for (Content c : Content.ACTIVITIES) {
                LocalCollection.deleteUri(c.uri, resolver);
            }
        } else {
            LocalCollection.deleteUri(contentToDelete.uri, resolver);
        }

        return resolver.delete(contentToDelete.uri, null, null);
    }

    public static Activity getLastActivity(Content content, ContentResolver resolver) {
        Activity a = null;
        Cursor c = resolver.query(content.uri,
                    null,
                    null,
                    null,
                    DBHelper.ActivityView.CREATED_AT + " ASC LIMIT 1");
        if (c != null && c.moveToFirst()){
            a = SoundCloudApplication.MODEL_MANAGER.getActivityFromCursor(c);
        }
        if (c != null) c.close();
        return a;
    }

    public static Activity getFirstActivity(Content content, ContentResolver resolver) {
        Activity a = null;
        Cursor c = resolver.query(content.uri,
                null,
                null,
                null,
                DBHelper.ActivityView.CREATED_AT + " DESC LIMIT 1");
        if (c != null && c.moveToFirst()) {
            a = SoundCloudApplication.MODEL_MANAGER.getActivityFromCursor(c);
        }
        if (c != null) c.close();
        return a;
    }

    public static Activities get(Content content, ContentResolver contentResolver) {
        return getSince(content, contentResolver, 0);
    }

    public static Activities getSince(Content content, ContentResolver resolver, long before)  {
        return getSince(content.uri, resolver, before);
    }
    public static Activities getSince(Uri contentUri, ContentResolver resolver, long since)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getSince("+contentUri+", since="+since+")");

        Activities activities = new Activities();
        LocalCollection lc = LocalCollection.fromContentUri(contentUri, resolver, false);
        if (lc != null) {
            activities.future_href = lc.extra;
        }
        Cursor c;
        if (since > 0) {
            c = resolver.query(contentUri,
                    null,
                    DBHelper.ActivityView.CREATED_AT+"> ?",
                    new String[] { String.valueOf(since) },
                    null);
        } else {
            c = resolver.query(contentUri, null, null, null, null);
        }
        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(c);
    }

    public static Activities getBefore(Uri contentUri, ContentResolver resolver, long before)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getBefore("+contentUri+", before="+before+")");
        Cursor c;
        if (before > 0) {
            c = resolver.query(contentUri,
                    null,
                    DBHelper.ActivityView.CREATED_AT+"< ?",
                    new String[] { String.valueOf(before) },
                    null);
        } else {
            c = resolver.query(contentUri, null, null, null, null);
        }

        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(c);
    }

    public static Activities get(ContentResolver resolver, Uri uri) {
        return get(resolver, uri, null, null, null, null);
    }

    public static Activities get(ContentResolver resolver, Uri uri, @Nullable String[] projection,
                                 @Nullable String where, @Nullable String[] whereArgs, @Nullable String sort) {
        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(resolver.query(uri, projection, where, whereArgs, sort));
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

    public Set<ScResource> getScResources() {
        final Set<ScResource> resources = new HashSet<ScResource>();
        for (Activity a : this) {
            Track t = a.getTrack();
            if (t != null ) {
                resources.add(t);
            }
            User u = a.getUser();
            if (u != null) {
                resources.add(u);
            }
        }
        return resources;
    }

    public List<Comment> getComments() {
        final List<Comment> comments = new ArrayList<Comment>();
        for (Activity a : this) {
            if (a instanceof CommentActivity){
                comments.add(((CommentActivity) a).comment);
            }
        }
        return comments;
    }

    public ContentValues[] getCommentContentValues() {
        return buildContentValues(getComments());
    }

    public static ContentValues[] buildContentValues(List<? extends ScModel> models) {
        ContentValues[] cv = new ContentValues[models.size()];
        for (int i=0; i<models.size(); i++) {
            cv[i] = models.get(i).buildContentValues();
        }
        return cv;
    }

    public int insert(Content content, ContentResolver resolver) {
//        SoundCloudApplication.MODEL_MANAGER.writeCollection(new ArrayList<ScResource>(getScResources()),
//                ScModel.CacheUpdateMode.MINI);

        Set<ScResource> models = new HashSet<ScResource>();
        for (Activity a : this) {
            models.addAll(a.getDependentModels());
        }

        Map<Uri, List<ContentValues>> values = new HashMap<Uri, List<ContentValues>>();
        for (ScResource m : models) {
            final Uri uri = m.getBulkInsertUri();
            if (values.get(uri) == null) {
                values.put(uri, new ArrayList<ContentValues>());
            }
            values.get(uri).add(m.buildContentValues());
        }

        for (Map.Entry<Uri, List<ContentValues>> entry : values.entrySet()) {
            resolver.bulkInsert(entry.getKey(), entry.getValue().toArray(new ContentValues[entry.getValue().size()]));
        }

        return resolver.bulkInsert(content.uri, buildContentValues(content.id));
    }

    public void mergeAndSort(Activities toMerge) {
        if (!toMerge.isEmpty()) collection.addAll(toMerge.collection);
        Collections.sort(collection);
    }

    public Set<String> artworkUrls() {
        Set<String> artworkUrls = new HashSet<String>();
        for (Activity a : this) {
            Track track = a.getTrack();
            if (track != null) {
                String artworkUrl = track.getArtwork();
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
            Track t = a.getTrack();
            if (t != null && t.shouldLoadIcon()) {
                return t.artwork_url;
            }
        }
        // no artwork found, fall back to avatar
        return getFirstAvailableAvatar();
    }
}
