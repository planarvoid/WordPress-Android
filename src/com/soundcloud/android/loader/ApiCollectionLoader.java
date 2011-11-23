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
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ApiCollectionLoader extends AsyncTaskLoader<ApiCollectionLoader.ApiResult> {

    private SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;

    private ApiResult mLastResult;
    private ApiResult mPendingResult;

    private Request mInitialRequest;
    private Class<?> mLoadModel;

    private boolean mLoading;

    public int pageSize;

    public ApiCollectionLoader(SoundCloudApplication app, Request initialRequest, Class<?> loadModel, ApiResult lastResult) {
        super(app);

        mApp = app;
        mInitialRequest = initialRequest;
        mLoadModel = loadModel;
        mLastResult = lastResult;
    }

    @Override public ApiResult loadInBackground() {

        final Context context = getContext();

        final Request req = mInitialRequest;
        if (req == null) throw new IllegalArgumentException("No request provided");

        ApiResult apiResponse = new ApiResult();
        try {
            HttpResponse resp = mApp.get(req);

            apiResponse.responseCode = resp.getStatusLine().getStatusCode();
            if (apiResponse.responseCode == HttpStatus.SC_NOT_MODIFIED) {
                return null;
            } else if (apiResponse.responseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            apiResponse.eTag = Http.etag(resp);

            InputStream is = resp.getEntity().getContent();
            List<Parcelable> results = new ArrayList<Parcelable>();
            CollectionHolder holder = getCollection(is, results);
            for (Parcelable p : results) {
                ((ModelBase) p).resolve(mApp);
            }
            apiResponse.nextHref = holder == null ? null : holder.next_href;
            apiResponse.items = results;

            return apiResponse;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return apiResponse;
        }

    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(ApiResult result) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.

            // do nothing for now, possible use this later
        }

        if (mLastResult != null){
            mLastResult.items.addAll(result.items);
            result.items = mLastResult.items;
            mLastResult = null;
        }


        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(result);
        } else {
            mPendingResult = result;
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mPendingResult != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mPendingResult);
        }



        if (takeContentChanged() || mLastResult != null) {
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
    @Override public void onCanceled(ApiResult response) {
        super.onCanceled(response);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();
        onStopLoading();
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


    public class ApiResult {
        public List<Parcelable> items;
        public String nextHref;
        public int responseCode;
        public boolean keepGoing;
        public String eTag;

        public ApiResult(){}
        public ApiResult(List<Parcelable> items, String nextHref, int responseCode, boolean keepGoing) {
            this.items = items;
            this.nextHref = nextHref;
            this.responseCode = responseCode;
            this.keepGoing = keepGoing;
        }
    }


}
