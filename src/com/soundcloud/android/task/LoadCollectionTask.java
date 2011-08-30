package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.ModelBase;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class LoadCollectionTask extends AsyncTask<Request, Parcelable, Boolean> {
    private SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ ArrayList<Parcelable> newItems = new ArrayList<Parcelable>();

    protected String mNextHref;
    protected int mResponseCode;

    public Class<?> loadModel;

    public int pageSize;
    protected String eTag;

    public LoadCollectionTask(SoundCloudApplication app, Class<?> loadModel) {
        mApp = app;
        this.loadModel = loadModel;
        Request.to(Endpoints.SUGGESTED_USERS);
    }

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     */
    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            loadModel = lazyEndlessAdapter.getLoadModel(false);
        }
    }

    @Override
    protected Boolean doInBackground(Request... request) {
        final Request req = request[0];
        if (req == null) return false;
        try {
            HttpResponse resp = mApp.get(req);

            mResponseCode = resp.getStatusLine().getStatusCode();
            if (mResponseCode == HttpStatus.SC_NOT_MODIFIED){
                return false;
            } else if (mResponseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }
            eTag = Http.etag(resp);

            InputStream is = resp.getEntity().getContent();

            CollectionHolder holder = getCollection(is);
            mNextHref = holder == null ? null : holder.next_href;

            if (newItems != null) {
                for (Parcelable p : newItems) {
                    ((ModelBase)p).resolve(mApp);
                }
                return !TextUtils.isEmpty(mNextHref);
            } else {
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    /* package */ CollectionHolder getCollection(InputStream is) throws IOException {
        CollectionHolder holder = null;
        if (Track.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, TracklistItemHolder.class);
            if (holder.size() > 0){
                newItems = new ArrayList<Parcelable>();
                for (TracklistItem t : (TracklistItemHolder) holder){
                    newItems.add(new Track(t));
                }
            }
        } else if (User.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, UserlistItemHolder.class);
            if (holder.size() > 0){
                newItems = new ArrayList<Parcelable>();
                for (UserlistItem u : (UserlistItemHolder) holder){
                    newItems.add(new User(u));
                }
            }
        } else if (Event.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, EventsHolder.class);
            if (holder.size() > 0){
                newItems = new ArrayList<Parcelable>();
                for (Event e : (EventsHolder) holder){
                    newItems.add(e);
                }
            }
        } else if (Friend.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, FriendHolder.class);
            if (holder.size() > 0){
                newItems = new ArrayList<Parcelable>();
                for (Friend f : (FriendHolder) holder){
                    newItems.add(f);
                }
            }
        }
        return holder;
    }

    public static class EventsHolder extends CollectionHolder<Event> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
    public static class FriendHolder extends CollectionHolder<Friend> {}
}
