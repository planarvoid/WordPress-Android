package com.soundcloud.android.task;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadCollectionTask extends AsyncTask<String, List<? super Parcelable>, Boolean> {
    protected SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();
    protected Params mParams;

    public boolean keepGoing;
    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;

    public static class Params {
        public Class<?> loadModel;
        public Uri contentUri;
        public int pageIndex;
        public boolean refresh;
        public Request request;
    }

    public LoadCollectionTask(SoundCloudApplication app, Params params) {
        mApp = app;
        mParams = params;
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            mParams.loadModel = lazyEndlessAdapter.getLoadModel(false);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success) respond();
    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {
        respond();
    }

    private void respond(){
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            if (mParams.refresh){
                adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
            } else {
                adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing);
            }
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (mParams.contentUri != null) {
            final Uri pagedUri = mParams.contentUri.buildUpon().appendQueryParameter("offset", String.valueOf(mParams.pageIndex * Consts.COLLECTION_PAGE_SIZE))
                    .appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build();
            Cursor itemsCursor = mApp.getContentResolver().query(pagedUri, null, null, null, null);
            // wipe it out and remote load ?? if (c.getCount() == localPageSize){ }
            mNewItems = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(mParams.loadModel)) {
                        mNewItems.add(new Track(itemsCursor));
                    } else if (User.class.equals(mParams.loadModel)) {
                        mNewItems.add(new User(itemsCursor));
                    } else if (Event.class.equals(mParams.loadModel)) {
                        mNewItems.add(new Event(itemsCursor));
                    } else if (Friend.class.equals(mParams.loadModel)) {
                        mNewItems.add(new User(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }

            keepGoing = mNewItems.size() == Consts.COLLECTION_PAGE_SIZE;
            publishProgress(mNewItems);
            if (itemsCursor != null) itemsCursor.close();
            return true;

        } else {
            // no local content, fail
            keepGoing = false;
            return false;
        }
    }

    /* package */ CollectionHolder getCollection(InputStream is, List<? super Parcelable> items) throws IOException {
        CollectionHolder holder = null;
        if (Track.class.equals(mParams.loadModel)) {
            holder = mApp.getMapper().readValue(is, TracklistItemHolder.class);
            for (TracklistItem t : (TracklistItemHolder) holder) {
                items.add(new Track(t));
            }
        } else if (User.class.equals(mParams.loadModel)) {
            holder = mApp.getMapper().readValue(is, UserlistItemHolder.class);
            for (UserlistItem u : (UserlistItemHolder) holder) {
                items.add(new User(u));
            }
        } else if (Event.class.equals(mParams.loadModel)) {
            holder = mApp.getMapper().readValue(is, EventsHolder.class);
            for (Event e : (EventsHolder) holder) {
                items.add(e);
            }
        } else if (Friend.class.equals(mParams.loadModel)) {
            holder = mApp.getMapper().readValue(is, FriendHolder.class);
            for (Friend f : (FriendHolder) holder) {
                items.add(f);
            }
        } else if (Comment.class.equals(mParams.loadModel)) {
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
