package com.soundcloud.android.model;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.*;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.*;

import static com.soundcloud.android.SoundCloudApplication.*;

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

    public Activities selectType(Activity.Type type) {
        List<Activity> activities = new ArrayList<Activity>();
        for (Activity e : this) {
            if (type.equals(e.type)) {
                activities.add(e);
            }
        }
        return new Activities(activities);
    }

    public Activities favoritings() {
        return selectType(Activity.Type.FAVORITING);
    }

    public Activities comments() {
        return selectType(Activity.Type.COMMENT);
    }

    public Activities sharings() {
        return selectType(Activity.Type.TRACK_SHARING);
    }

    public Activities tracks() {
        return selectType(Activity.Type.TRACK);
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

    @SuppressWarnings("UnusedDeclaration")
    public Activities toJSON(File f, Class<?> view) throws IOException {
        AndroidCloudAPI.Mapper.viewWriter(Views.Mini.class).writeValue(f, this);
        return this;
    }

    // TODO, get rid of future href and next href and generate them
    public Activities merge(Activities old) {
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
        boolean caughtUp = false;
        String future_href = null;
        String next_href;
        List<Activity> activityList = new ArrayList<Activity>();
        Request remote = new Request(request).add("limit", 20);
        do {
            Log.d(TAG, "Making request " + remote);
            HttpResponse response = api.get(remote);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Activities activities = fromJSON(response.getEntity().getContent());
                remote = activities.hasMore() ? activities.getNextRequest() : null;
                if (future_href == null && !TextUtils.isEmpty(activities.future_href)) {
                    future_href = URLDecoder.decode(activities.future_href);
                }
                next_href = activities.next_href;

                Log.d(TAG,"Got events " + activities.size());

                for (Activity evt : activities) {
                    if (max < 0 || activityList.size() < max) {
                        activityList.add(evt);
                    } else {
                        caughtUp = true;
                        break;
                    }
                }
            } else {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    Log.d(TAG, "Got no content response (204)");
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

    public static Activities fetch(AndroidCloudAPI api,
                                   final Request request) throws IOException {
        HttpResponse response = api.get(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return fromJSON(response.getEntity().getContent());
        } else {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                Log.d(TAG, "Got no content response (204)");
                return EMPTY;
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                        response.getStatusLine().getReasonPhrase());
            } else {
                throw new IOException(response.getStatusLine().toString());
            }
        }
    }

    public static int clear(Content content, ContentResolver resolver) {
        Content contentToDelete = Content.ME_ALL_ACTIVITIES;
        if (content != null) {
            contentToDelete = content;
        }
        if (!Activity.class.isAssignableFrom(contentToDelete.resourceType)) {
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
            a = new Activity(c);
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
        while (c != null && c.moveToNext()) {
            activities.add(new Activity(c));
        }
        if (c != null) c.close();
        return activities;
    }

    public static Activities getBefore(Content content, ContentResolver resolver, long before)  {
        return getBefore(content.uri, resolver, before);
    }
    public static Activities getBefore(Uri contentUri, ContentResolver resolver, long before)  {
        Log.d(TAG, "Activities.getBefore("+contentUri+", before="+before+")");
        Activities activities = new Activities();
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

        while (c != null && c.moveToNext()) {
            final long id = c.getLong(c.getColumnIndex(DBHelper.ActivityView._ID));
            activities.add(new Activity(c));
        }
        if (c != null) c.close();
        return activities;
    }

    public static Activities get(ContentResolver resolver, Uri uri) {
        return get(resolver,uri,null,null,null,null);
    }

    public static Activities get(ContentResolver resolver, Uri uri, String[] projection, String where, String[] whereArgs, String sort) {
        Activities activities = new Activities();
        Cursor c = resolver.query(uri, projection, where, whereArgs, sort);
        while (c != null && c.moveToNext()) {
            activities.add(new Activity(c));
        }
        if (c != null) c.close();
        return activities;
    }

    public static List<Long> getLocalIdsSince(ContentResolver resolver, Uri contentUri, long since)  {
        return getLocalIds(resolver, contentUri, DBHelper.ActivityView.CREATED_AT + "> ?", new String[]{String.valueOf(since)}, null);
    }

    public static List<Long> getLocalIds(ContentResolver resolver, Uri uri) {
        return getLocalIds(resolver,uri,null,null,null);
    }

    public static List<Long> getLocalIds(ContentResolver resolver, Uri uri, String where, String[] whereArgs, String sort) {
        List<Long> ids = new ArrayList<Long>();
        Cursor c = resolver.query(uri, new String[]{DBHelper.ActivityView._ID}, where, whereArgs, sort);
        while (c != null && c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        if (c != null) c.close();
        return ids;
    }

    public static List<Long> getActivities(Uri uri, ContentResolver resolver) {
        List<Long> ids = new ArrayList<Long>();
        Cursor c = resolver.query(uri, new String[]{DBHelper.ActivityView._ID}, null, null, null);
        while (c != null && c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        if (c != null) c.close();
        return ids;
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

    public List<Track> getTracks() {
        final List<Track> tracks = new ArrayList<Track>();
        for (Activity a : this) {
            Track t = a.getTrack();
            if (t != null && !tracks.contains(t)) {
                tracks.add(t);
            }
        }
        return tracks;
    }

    public List<User> getUsers() {
        final List<User> users = new ArrayList<User>();
        for (Activity a : this) {
            User u = a.getUser();
            if (u != null && !users.contains(u)) {
                users.add(u);
            }
        }
        return users;
    }

    public List<Comment> getComments() {
        final List<Comment> comments = new ArrayList<Comment>();
        for (Activity a : selectType(Activity.Type.COMMENT)) {
            Comment c = a.getComment();
            if (c != null && !comments.contains(c)) {
                comments.add(c);
            }
        }
        return comments;
    }

    public ContentValues[] getTrackContentValues() {
        return buildContentValues(getTracks());
    }

    public ContentValues[] getUserContentValues() {
        return buildContentValues(getUsers());
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
        resolver.bulkInsert(Content.TRACKS.uri, getTrackContentValues());
        resolver.bulkInsert(Content.USERS.uri, getUserContentValues());

        if (content == Content.ME_ACTIVITIES || content == Content.ME_ALL_ACTIVITIES) {
            resolver.bulkInsert(Content.COMMENTS.uri, getCommentContentValues());
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
}
