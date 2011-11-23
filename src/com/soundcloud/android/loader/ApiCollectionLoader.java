package com.soundcloud.android.loader;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.ModelBase;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ApiCollectionLoader extends AsyncTaskLoader<List<Parcelable>> {

    private SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ List<Parcelable> mResults = new ArrayList<Parcelable>();

    protected String mNextHref;
    protected int mResponseCode;

    private Request mInitialRequest;
    private Class<?> mLoadModel;


    public int pageSize;
    protected String eTag;

    public ApiCollectionLoader(SoundCloudApplication app, Request initialRequest, Class<?> loadModel) {
        super(app);

        mApp = app;
        mInitialRequest = initialRequest;
        mLoadModel = loadModel;
    }

    public boolean hasNextPage(){
        return (!TextUtils.isEmpty(mNextHref));
    }

    public boolean nextPage(){
        if (hasNextPage()){

            forceLoad();
        }
        return false;
    }

    @Override public List<Parcelable> loadInBackground() {

        final Context context = getContext();

        final Request req = TextUtils.isEmpty(mNextHref) ? mInitialRequest : new Request(mNextHref);
        if (req == null) throw new IllegalArgumentException("No request provided");

        try {

            HttpResponse resp = mApp.get(req);

            mResponseCode = resp.getStatusLine().getStatusCode();
            if (mResponseCode == HttpStatus.SC_NOT_MODIFIED) {
                return null;
            } else if (mResponseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }
            eTag = Http.etag(resp);

            InputStream is = resp.getEntity().getContent();

            CollectionHolder holder = getCollection(is, mResults);
            mNextHref = holder == null ? null : holder.next_href;

            if (mResults != null) {
                for (Parcelable p : mResults) {
                    ((ModelBase)p).resolve(mApp);
                }
            }
            return mResults;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }

    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(List<Parcelable> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }

        List<Parcelable> oldApps = apps;
        mResults = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mResults != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mResults);
        }


        if (takeContentChanged() || mResults == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override public void onCanceled(List<Parcelable> results) {
        super.onCanceled(results);
        onReleaseResources(results);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();

        if (mResults != null){
            onReleaseResources(mResults);
        }
        onStopLoading();
    }

    protected void onReleaseResources(List<Parcelable> results) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }


     /* package */ CollectionHolder getCollection(InputStream is, List<? super Parcelable> items) throws IOException {
        CollectionHolder holder = null;
        if (Track.class.equals(mLoadModel)) {
            holder = mApp.getMapper().readValue(is, TracklistItemHolder.class);
            for (TracklistItem t : (TracklistItemHolder) holder) {
                items.add(new Track(t));
            }
        } else if (User.class.equals(mLoadModel)) {
            holder = mApp.getMapper().readValue(is, UserlistItemHolder.class);
            for (UserlistItem u : (UserlistItemHolder) holder) {
                items.add(new User(u));
            }
        } else if (Event.class.equals(mLoadModel)) {
            holder = mApp.getMapper().readValue(is, EventsHolder.class);
            for (Event e : (EventsHolder) holder) {
                items.add(e);
            }
        } else if (Friend.class.equals(mLoadModel)) {
            holder = mApp.getMapper().readValue(is, FriendHolder.class);
            for (Friend f : (FriendHolder) holder) {
                items.add(f);
            }
        } else if (Comment.class.equals(mLoadModel)) {
            holder = mApp.getMapper().readValue(is, CommentHolder.class);
            for (Comment f : (CommentHolder) holder) {
                items.add(f);
            }
        }
        return holder;
    }

    public static class EventsHolder extends CollectionHolder<Event> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
    public static class FriendHolder extends CollectionHolder<Friend> {}
    public static class CommentHolder extends CollectionHolder<Comment> {}


}
