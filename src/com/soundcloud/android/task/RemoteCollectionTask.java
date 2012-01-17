package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.model.LocalCollection.insertLocalCollection;
import static com.soundcloud.android.service.sync.ApiSyncer.getAdditionsFromIds;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.RemoteCollectionAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionTask extends AsyncTask<RemoteCollectionTask.CollectionParams, List<? super Parcelable>, Boolean> {

    protected SoundCloudApplication mApp;
    protected CollectionParams mParams;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    public boolean keepGoing;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    private long mLastRefresh;
    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;

    public static class CollectionParams {
        public Class<?> loadModel;
        public Uri contentUri;
        public int pageIndex;
        public boolean isRefresh;
        public Request request;
        public boolean refreshPageItems;

        @Override
        public String toString() {
            return "CollectionParams{" +
                    "loadModel=" + loadModel +
                    ", contentUri=" + contentUri +
                    ", pageIndex=" + pageIndex +
                    ", isRefresh=" + isRefresh +
                    ", request=" + request +
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

    public void setLastRefresh(long lastRefresh) {
        mLastRefresh = lastRefresh;
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
        RemoteCollectionAdapter adapter = (RemoteCollectionAdapter) mAdapterReference.get();
        if (adapter != null) {
            if (mParams.isRefresh){
                adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
            } else {
                adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing);
            }
        }
    }

    @Override
    protected Boolean doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading collection with params: " + mParams);

        if (mParams.contentUri == null && mParams.request != null) {
            return doRemoteLoad();

        } else if (mParams.contentUri != null) {

            LocalData localData = new LocalData(mApp.getContentResolver(), mParams);
            if (mParams.request != null) {
                if (mParams.refreshPageItems) {
                    // TODO stale check??, failure check??
                    refreshLocalItems(mApp.getContentResolver(), localData);
                }
            }
            keepGoing = localData.idList.size() == Consts.COLLECTION_PAGE_SIZE;
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

            Log.i(TAG, getClass().getSimpleName() + " got response " + resp);

            // process new items and publish them
            CollectionHolder holder = ScModel.getCollectionFromStream(resp.getEntity().getContent(), mApp.getMapper(), mParams.loadModel, mNewItems);
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

    private boolean refreshLocalItems(ContentResolver resolver, LocalData localData) {
        try {

            final String resourceUrl = mParams.request.toUrl();
            Request idRequest = new Request(resourceUrl.substring(0, resourceUrl.indexOf("?")) + "/ids");
            for (String key : mParams.request.getParams().keySet()){
                idRequest.add(key,mParams.request.getParams().get(key));
            }

            if (localData.localCollectionPage != null) localData.localCollectionPage.applyEtag(idRequest);

            HttpResponse resp = mApp.get(idRequest);
            mResponseCode = resp.getStatusLine().getStatusCode();

            Log.i(TAG, getClass().getSimpleName() + " got id response " + resp);
            if (mResponseCode != HttpStatus.SC_OK && mResponseCode != HttpStatus.SC_NOT_MODIFIED) {
                throw new IOException("Invalid response: " + resp.getStatusLine());

            } else {
                List<Long> ids;

                if (mResponseCode == HttpStatus.SC_OK) {

                    // create updated id list
                    localData.idList = new ArrayList<Long>();
                    ApiSyncer.IdHolder holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ApiSyncer.IdHolder.class);
                    if (holder.collection != null) localData.idList.addAll(holder.collection);

                    // update the new page
                    LocalCollection.deletePagesFrom(resolver, localData.localCollection.id,mParams.pageIndex);
                    LocalCollectionPage lcp = new LocalCollectionPage(localData.localCollection.id, mParams.pageIndex, localData.idList.size(), Http.etag(resp));
                    resolver.insert(Content.COLLECTION_PAGES.uri, lcp.toContentValues());
                    Log.i(TAG, getClass().getSimpleName() + " inserted local page " + lcp);

                    ContentValues[] cv = new ContentValues[localData.idList.size()];
                    int i = 0;
                    final long userId = mApp.getCurrentUserId();
                    for (Long id : localData.idList) {
                        cv[i] = new ContentValues();
                        cv[i].put(DBHelper.CollectionItems.POSITION, mParams.pageIndex * Consts.COLLECTION_PAGE_SIZE + i + 1);
                        cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
                        cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
                        i++;
                    }
                    int added = resolver.bulkInsert(mParams.contentUri, cv);

                    Log.i(TAG, getClass().getSimpleName() + " inserted ids, size " + added);

                }

            }
            if (mParams.pageIndex == 0) localData.localCollection.updateLastSyncTime(System.currentTimeMillis(), resolver);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        return false;
    }

    private int insertMissingItems(List<Long> pageIds) throws IOException {
        Content c = Content.match(mParams.contentUri);
        return SoundCloudDB.bulkInsertParcelables(
                mApp.getContentResolver(),
                getAdditionsFromIds(mApp, mApp.getContentResolver(), pageIds, c, false));
    }

    private long getCollectionOwner() {
        if (Content.match(mParams.contentUri).isMine()) {
            return mApp.getCurrentUserId();
        } else if (mParams.contentUri.getPathSegments().size() > 2){
            try {
                return Long.parseLong(mParams.contentUri.getPathSegments().get(1));
            } catch (NumberFormatException e){
                return mApp.getCurrentUserId();
            }
        } else{
            return -1;
        }
    }

    private static class LocalData {
        LocalCollection localCollection;
        LocalCollectionPage localCollectionPage;
        List<Long> idList;

        public LocalData(ContentResolver contentResolver, CollectionParams mParams) {
            localCollectionPage = null;
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(mParams.contentUri, contentResolver);
            if (localCollection == null) {
                localCollection = insertLocalCollection(mParams.contentUri, contentResolver);
                 idList = new ArrayList<Long>();
            } else {
                idList = Content.match(mParams.contentUri).getStoredIds(contentResolver,mParams.pageIndex);
                // look for content page and check its size against the DB
                localCollectionPage = LocalCollectionPage.fromCollectionAndIndex(contentResolver, localCollection.id, mParams.pageIndex);
                if (localCollectionPage != null && (idList == null || idList.size() != localCollectionPage.size)) {
                    localCollectionPage = null;
                }
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
        Cursor itemsCursor = mApp.getContentResolver().query(CloudUtils.getPagedUri(mParams.contentUri, mParams.pageIndex), null, null, null, null);
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


}
