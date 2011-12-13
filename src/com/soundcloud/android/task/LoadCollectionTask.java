package com.soundcloud.android.task;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;

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
            mNewItems = (List<Parcelable>) loadLocalContent();
            keepGoing = mNewItems.size() == Consts.COLLECTION_PAGE_SIZE;
            publishProgress(mNewItems);
            return true;
        } else {
            // no local content, fail
            keepGoing = false;
            return false;
        }
    }

    protected List<? super Parcelable> loadLocalContent(){
        Cursor itemsCursor = mApp.getContentResolver().query(getPagedUri(), null, null, null, null);
            // wipe it out and remote load ?? if (c.getCount() == localPageSize){ }
            List<Parcelable> items = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(mParams.loadModel)) {
                        items.add(new Track(itemsCursor));
                    } else if (User.class.equals(mParams.loadModel)) {
                        items.add(new User(itemsCursor));
                    } else if (Friend.class.equals(mParams.loadModel)) {
                        items.add(new User(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }
        if (itemsCursor != null) itemsCursor.close();
        return items;
    }

    protected Uri getPagedUri(){
        return  mParams.contentUri == null ? null :
                mParams.contentUri.buildUpon().appendQueryParameter("offset", String.valueOf(mParams.pageIndex * Consts.COLLECTION_PAGE_SIZE))
                    .appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build();
    }


}
