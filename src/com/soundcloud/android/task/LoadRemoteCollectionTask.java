package com.soundcloud.android.task;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.RemoteCollectionAdapter;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.model.LocalCollection.insertLocalCollection;
import static com.soundcloud.android.service.sync.ApiSyncer.getAdditionsFromIds;
import static com.soundcloud.android.service.sync.ApiSyncer.idCursorToList;

public class LoadRemoteCollectionTask extends LoadCollectionTask {

    private long mLastRefresh;
    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;
    private final Request mRequest;

    public LoadRemoteCollectionTask(SoundCloudApplication app, LazyEndlessAdapter adapter, Request request) {
        super(app, adapter);
        mRequest = request;
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
            if (mParams.refresh){
                adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
            } else {
                adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing);
            }
        }
    }

    @Override
    protected Boolean doInBackground(Boolean... params) {

        final boolean refresh = params != null && params.length > 0 ? params[0] : true;

        Log.i(TAG, getClass().getSimpleName() + " refresh " + refresh + " " + params);

        LocalData localData = null;
        if (refresh && mParams.contentUri != null){
            localData = new LocalData(mApp.getContentResolver(), mParams, mRequest);
        }

        Log.i(TAG, getClass().getSimpleName() + " local data " + localData);

        if (mParams.contentUri == null){
            return loadFullRemote(mApp.getContentResolver(),localData);
        } else {

            boolean success = true;
            if (refresh) {
                success = refreshLocalItems(mApp.getContentResolver(), localData);
            }

            // at this point, we either have good local content, or something failed
            if (success && mParams.contentUri != null) {
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
    }

    private boolean loadFullRemote(ContentResolver resolver, LocalData localData) {
        try {

            HttpResponse resp = mApp.get(mRequest);
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


            final String resourceUrl = mRequest.toUrl();
            Request idRequest = new Request(resourceUrl.substring(0, resourceUrl.indexOf("?")) + "/ids");
            for (String key : mRequest.getParams().keySet()){
                idRequest.add(key,mRequest.getParams().get(key));
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

                    // update the new page
                    LocalCollection.deletePagesFrom(resolver, localData.localCollection.id,mParams.pageIndex);
                    LocalCollectionPage lcp = new LocalCollectionPage(localData.localCollection.id, mParams.pageIndex, mNewItems.size(), mNextHref, Http.etag(resp));
                    resolver.insert(Content.COLLECTION_PAGES.uri, lcp.toContentValues());

                    Log.i(TAG, getClass().getSimpleName() + " inserted local page " + lcp);

                   // create updated id list
                    ids = new ArrayList<Long>();
                    ApiSyncer.IdHolder holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ApiSyncer.IdHolder.class);
                    if (holder.collection != null) ids.addAll(holder.collection);


                    ContentValues[] cv = new ContentValues[ids.size()];
                    int i = 0;
                    final long userId = mApp.getCurrentUserId();
                    for (Long id : ids) {
                        cv[i] = new ContentValues();
                        cv[i].put(DBHelper.CollectionItems.POSITION, mParams.pageIndex * Consts.COLLECTION_PAGE_SIZE + i + 1);
                        cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
                        cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
                        i++;
                    }
                    int added = resolver.bulkInsert(mParams.contentUri, cv);

                    Log.i(TAG, getClass().getSimpleName() + " inserted ids, size " + added);

                } else {
                    ids = localData.idList;
                }

                int added = insertMissingItems(ids);

                Log.i(TAG, getClass().getSimpleName() + " added missing parcelables, size " + added);

            }
            if (mParams.pageIndex == 0) localData.localCollection.updateLasySyncTime(resolver, System.currentTimeMillis());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        return false;
    }

    private int insertMissingItems(List<Long> pageIds) throws IOException {
        Content c = Content.match(mParams.contentUri);
        final int itemCount = pageIds.size();
        return SoundCloudDB.bulkInsertParcelables(mApp, getAdditionsFromIds(mApp, pageIds, c.resourceType, false));
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

        public LocalData(ContentResolver contentResolver, CollectionParams mParams, Request request) {
            localCollectionPage = null;
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(contentResolver, mParams.contentUri);

            if (localCollection == null) {
                localCollection = insertLocalCollection(contentResolver, mParams.contentUri);
            } else {
                // look for content page and check its size against the DB
                localCollectionPage = LocalCollectionPage.fromCollectionAndIndex(contentResolver, localCollection.id, mParams.pageIndex);
                if (localCollectionPage != null) {
                    idList = idCursorToList(contentResolver.query(CloudUtils.getPagedUri(mParams.contentUri, mParams.pageIndex), new String[]{DBHelper.TrackView._ID}, null, null, null));
                    if (idList == null || idList.size() != localCollectionPage.size) {
                        localCollectionPage = null;
                    }
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

}
