package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.*;
import static com.soundcloud.android.model.LocalCollection.insertLocalCollection;
import static com.soundcloud.android.service.sync.ApiSyncer.getAdditionsFromIds;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.RemoteCollectionAdapter;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionTask extends AsyncTask<Object, List<? super Parcelable>, Boolean>
                                  implements ILazyAdapterTask {

    protected SoundCloudApplication mApp;
    protected CollectionParams mParams;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    public boolean keepGoing;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;

    public static class CollectionParams {
        public Class<?> loadModel;
        public Uri contentUri;
        public boolean isRefresh;
        public Request request;
        public boolean refreshPageItems;
        public int startIndex;
        public int maxToLoad;

        @Override
        public String toString() {
            return "CollectionParams{" +
                    "loadModel=" + loadModel +
                    ", contentUri=" + contentUri +
                    ", isRefresh=" + isRefresh +
                    ", request=" + request +
                    ", maxToLoad=" + maxToLoad +
                    '}';
        }
    }

    public RemoteCollectionTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        mApp = app;
        setAdapter(lazyEndlessAdapter);
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success) respond();
    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {
        respond();
    }

    protected void respond(){
        RemoteCollectionAdapter adapter = (RemoteCollectionAdapter) mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing, mParams.isRefresh);
        }
    }

    @Override
    protected Boolean doInBackground(Object... params) {
        mParams = (CollectionParams) params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading collection with params: " + mParams);

        if (mParams.contentUri == null && mParams.request != null) {
            return doRemoteLoad();

        } else if (mParams.contentUri != null) {

            LocalData localData = new LocalData(mApp, mParams);
            keepGoing = localData.idList.size() == mParams.maxToLoad;
            try {
                insertMissingItems(localData.idList);
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }

            mNewItems = (List<Parcelable>) loadLocalContent();
            for (Parcelable p : mNewItems) {
                ((ScModel) p).resolve(mApp);
            }
            publishProgress(mNewItems);
            return true;

        } else {
            // no local content, fail
            keepGoing = false;
            return false;
        }
    }

    protected boolean doRemoteLoad() {
        try {
            HttpResponse resp = mApp.get(mParams.request);
            mResponseCode = resp.getStatusLine().getStatusCode();
            if (mResponseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            // process new items and publish them
            CollectionHolder holder = ScModel.getCollectionFromStream(resp.getEntity().getContent(), mApp.getMapper(),
                    mParams.loadModel, mNewItems);
            mNextHref = holder == null || TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
            keepGoing = !TextUtils.isEmpty(mNextHref);
            for (Parcelable p : mNewItems) {
                ((ScModel) p).resolve(mApp);
            }

            // publish what we have, since we already have the items, we don't have to wait on a db commit
            publishProgress(mNewItems);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }
        return false;
    }

    private int insertMissingItems(List<Long> pageIds) throws IOException {
        Content c = Content.match(mParams.contentUri);
        return SoundCloudDB.bulkInsertParcelables(
                mApp.getContentResolver(),
                getAdditionsFromIds(mApp, mApp.getContentResolver(), pageIds, c, false));
    }

    private static class LocalData {
        LocalCollection localCollection;
        LocalCollectionPage localCollectionPage;
        List<Long> idList;

        public LocalData(SoundCloudApplication app, CollectionParams mParams) {
            localCollectionPage = null;
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(mParams.contentUri, app.getContentResolver(), false);
            if (localCollection == null) {
                localCollection = insertLocalCollection(mParams.contentUri, app.getContentResolver());
                 idList = new ArrayList<Long>();
            } else {
                idList = Content.match(mParams.contentUri).getLocalIds(app.getContentResolver(), SoundCloudApplication.getUserId(), mParams.startIndex, mParams.maxToLoad);
            }
        }

        @Override
        public String toString() {
            return "LocalData{" +
                    "localCollection=" + localCollection +
                    ", localCollectionPage=" + localCollectionPage +
                    ", idList=" + idList +
                    '}';
        }
    }

    protected List<? extends Parcelable> loadLocalContent(){
        Cursor itemsCursor = mApp.getContentResolver().query(
                SoundCloudDB.addPagingParams(mParams.contentUi, mParams.startIndex, mParams.maxToLoad)
                , null, null, null, null);
            List<Parcelable> items = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(mParams.loadModel)) {
                        final Parcelable t = TRACK_CACHE.fromCursor(itemsCursor);
                        items.add(t);
                    } else if (User.class.equals(mParams.loadModel)) {
                        items.add(USER_CACHE.fromCursor(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }
        if (itemsCursor != null) itemsCursor.close();
        return items;
    }
}
