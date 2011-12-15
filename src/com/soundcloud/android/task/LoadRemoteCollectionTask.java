package com.soundcloud.android.task;

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
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

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
        boolean loadRemote = params[0];

        Cursor c = null;
        LocalCollection localCollection = null;
        LocalCollectionPage localCollectionPage = null;

        if (loadRemote && mParams.contentUri != null){
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(mApp.getContentResolver(), mParams.contentUri);
            if (localCollection == null) {
                localCollection = com.soundcloud.android.model.LocalCollection.insertLocalCollection(mApp.getContentResolver(), mParams.contentUri);
            } else {
                localCollectionPage = LocalCollectionPage.fromCollectionAndIndex(mApp.getContentResolver(), localCollection.id, mParams.pageIndex);
                final long start = System.currentTimeMillis();
                Cursor itemsCursor = mApp.getContentResolver().query(CloudUtils.getPagedUri(mParams.contentUri, mParams.pageIndex), new String[]{DBHelper.TrackView._ID}, null, null, null);
                if (localCollectionPage != null) {
                    if (itemsCursor == null || itemsCursor.getCount() != localCollectionPage.size) {
                        localCollectionPage = null;
                    } else {
                        localCollectionPage.applyEtag(mRequest);
                    }
                }
            }
        }

        // fetch if there is no local uri, no stored colleciton for this page,
        if (mParams.contentUri == null || localCollectionPage == null || loadRemote) {
            try {
                HttpResponse resp = mApp.get(mRequest);
                mResponseCode = resp.getStatusLine().getStatusCode();
                if (mResponseCode != HttpStatus.SC_OK && mResponseCode != HttpStatus.SC_NOT_MODIFIED) {
                    throw new IOException("Invalid response: " + resp.getStatusLine());
                }

                if (mResponseCode == HttpStatus.SC_OK) {

                    // we have new content. wipe out everything for now (or it gets tricky)
                    if (localCollection != null) {
                        mApp.getContentResolver().delete(Content.COLLECTION_PAGES.uri,
                                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                                new String[]{String.valueOf(localCollection.id), String.valueOf(mParams.pageIndex)});
                    }

                    // process new items and publish them
                    CollectionHolder holder = ScModel.getCollectionFromStream(resp.getEntity().getContent(), mApp.getMapper(), mParams.loadModel, mNewItems);
                    mNextHref = holder == null || TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
                    keepGoing = !TextUtils.isEmpty(mNextHref);
                    for (Parcelable p : mNewItems) {
                        ((ScModel) p).resolve(mApp);
                    }
                    // publish what we have, commit new items in the background, no need to wait
                    publishProgress(mNewItems);

                    // store items if we have items and a content uri
                    if (mParams.contentUri != null && mNewItems != null) {

                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.CollectionPages.COLLECTION_ID, localCollection.id);
                        cv.put(DBHelper.CollectionPages.PAGE_INDEX, mParams.pageIndex);
                        cv.put(DBHelper.CollectionPages.ETAG, Http.etag(resp));
                        cv.put(DBHelper.CollectionPages.SIZE, mNewItems.size());
                        if (mNextHref != null) {
                            cv.put(DBHelper.CollectionPages.NEXT_HREF, mNextHref);
                        }

                        // insert new page
                        mApp.getContentResolver().insert(Content.COLLECTION_PAGES.uri, cv);
                        SoundCloudDB.bulkInsertParcelables(mApp,mNewItems,mParams.contentUri,getCollectionOwner(),
                                mParams.pageIndex * Consts.COLLECTION_PAGE_SIZE);
                    }
                    return true;
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }
        }

        // if we get this far, we either failed (return false), or our etags matched up (load locally)
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
}
