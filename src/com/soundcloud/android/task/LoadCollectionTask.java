package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoadCollectionTask extends AsyncTask<String, List<? super Parcelable>, Boolean> {
    protected SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    public Uri mContentUri;
    public Class<?> mLoadModel;
    public boolean mRefresh;
    public int mPageIndex;

    public boolean keepGoing;
    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;

    public LoadCollectionTask(SoundCloudApplication app, Class<?> loadModel, Uri contentUri, int pageIndex, boolean refresh) {
        mApp = app;
        mLoadModel = loadModel;
        mContentUri = contentUri;
        mPageIndex = pageIndex;
        mRefresh = refresh;
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            mLoadModel = lazyEndlessAdapter.getLoadModel(false);
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
            Log.i("asdf","Sending back response code of " + mResponseCode);
            if (mRefresh){
                adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
            } else {
                adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing);
            }
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (mContentUri != null) {
            final Uri pagedUri = mContentUri.buildUpon().appendQueryParameter("offset", String.valueOf(mPageIndex * Consts.COLLECTION_PAGE_SIZE))
                    .appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build();
            Cursor itemsCursor = mApp.getContentResolver().query(pagedUri, null, null, null, null);

            // wipe it out and remote load ?? if (c.getCount() == localPageSize){ }
            mNewItems = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(mLoadModel)) {
                        mNewItems.add(new Track(itemsCursor));
                    } else if (User.class.equals(mLoadModel)) {
                        mNewItems.add(new User(itemsCursor));
                    } else if (Event.class.equals(mLoadModel)) {
                        mNewItems.add(new Event(itemsCursor));
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
